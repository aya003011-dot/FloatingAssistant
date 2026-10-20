import express from 'express';

const app = express();
app.disable('x-powered-by');
app.use(express.json({ limit: '12kb' }));
const PORT = Number(process.env.PORT || 8080);
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;
const ACCESS_TOKEN = process.env.ASSISTANT_ACCESS_TOKEN;
const MODEL = process.env.OPENAI_MODEL || 'gpt-6-astra';

// Small in-memory rate limit: 25 calls per IP per minute.
const buckets = new Map();
app.use((req, res, next) => {
  const now = Date.now();
  const key = req.ip || 'unknown';
  let b = buckets.get(key);
  if (!b || now - b.start > 60_000) b = { start: now, count: 0 };
  b.count += 1;
  buckets.set(key, b);
  if (b.count > 25) return res.status(429).json({ error: 'too_many_requests' });
  next();
});

app.get('/health', (_req, res) => res.json({ ok: true }));
app.post('/command', async (req, res) => {
  if (!ACCESS_TOKEN) return res.status(503).json({ error: 'server_not_configured' });
  if (req.get('X-Assistant-Token') !== ACCESS_TOKEN) return res.status(401).json({ error: 'unauthorized' });
  if (!OPENAI_API_KEY) return res.status(503).json({ error: 'openai_key_not_configured' });

  const text = typeof req.body?.text === 'string' ? req.body.text.trim().slice(0, 1000) : '';
  const installedApps = Array.isArray(req.body?.installedApps)
    ? req.body.installedApps.filter(x => typeof x === 'string').slice(0, 100).map(x => x.slice(0, 80))
    : [];
  if (!text) return res.status(400).json({ error: 'empty_command' });

  const instructions = `You are the language-understanding brain of a Persian Android floating assistant. Interpret the user's command and return ONLY a JSON object with keys: action, app, text, message. Allowed action values: open_app, click_text, type_text, back, home, hide, search_web, search_youtube, answer. Use short, clear Persian messages. The installed app labels are supplied by the user's device; for open_app choose the closest exact installed label. If no matching app exists, use answer and explain briefly. For commands that ask to send a Telegram/SMS/social message, only open the requested messaging app and tell the user to choose/check the recipient and press Send themselves; never click Send or submit a message for them. For VPN connection requests, open the matching VPN app but tell the user to review its Connect button and any Android system consent themselves; never bypass Android consent. Do not claim an action succeeded unless the client can perform it. click_text is only for a clearly named visible control the user explicitly asks to click. type_text is only for text the user explicitly asked to enter; do not submit it. Never install apps, buy anything, delete data, change security settings, or send/submit anything automatically. For ordinary questions, use action=answer and put the response in message. For open_app, use app=an installed app label and message as a brief confirmation. For search_web/search_youtube put the search query in text. For click_text put exact visible text in text. For type_text put requested text in text. For hide/back/home the other fields can be empty. Output valid JSON only.`;

  try {
    const upstream = await fetch('https://api.openai.com/v1/responses', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${OPENAI_API_KEY}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        model: MODEL,
        instructions,
        input: JSON.stringify({ userCommand: text, installedAppLabels: installedApps }),
        text: { format: { type: 'json_object' } },
        max_output_tokens: 250
      }),
      signal: AbortSignal.timeout(25_000)
    });
    if (!upstream.ok) return res.status(502).json({ error: 'ai_request_failed', upstream_status: upstream.status });
    const data = await upstream.json();
    let output = data.output_text;
    if (!output && Array.isArray(data.output)) {
      output = data.output.flatMap(item => item.content || [])
        .filter(part => part.type === 'output_text')
        .map(part => part.text || '').join('');
    }
    if (typeof output !== 'string') throw new Error('No output text');
    const parsed = JSON.parse(output);
    const allowed = new Set(['open_app','click_text','type_text','back','home','hide','search_web','search_youtube','answer']);
    const action = allowed.has(parsed.action) ? parsed.action : 'answer';
    const result = {
      action,
      app: typeof parsed.app === 'string' ? parsed.app.slice(0, 100) : '',
      text: typeof parsed.text === 'string' ? parsed.text.slice(0, 500) : '',
      message: typeof parsed.message === 'string' ? parsed.message.slice(0, 500) : ''
    };
    if (action === 'open_app' && !installedApps.some(label => label.toLocaleLowerCase() === result.app.toLocaleLowerCase())) {
      result.action = 'answer';
      result.message = 'برنامه‌ای با این نام بین برنامه‌های قابل‌اجرا پیدا نکردم. اسمش را دقیق‌تر بگو.';
      result.app = '';
    }
    res.json(result);
  } catch (_error) {
    res.status(502).json({ error: 'ai_request_failed' });
  }
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Floating Assistant backend listening on ${PORT}`);
  if (!OPENAI_API_KEY) console.warn('OPENAI_API_KEY is not configured');
  if (!ACCESS_TOKEN) console.warn('ASSISTANT_ACCESS_TOKEN is not configured; /command is disabled');
});
