let sound = false;
let orders = [];
let calls = [];
const LABEL = {NEW: 'Naya order', PREPARING: 'Ban raha hai', READY: 'Taiyaar'};
const NEXT = {NEW: ['PREPARING', 'Banana shuru karo'], PREPARING: ['READY', 'Taiyaar hai'], READY: ['SERVED', 'Parosa gaya']};

function speak(text) {
  if (!sound || !('speechSynthesis' in window)) return;
  const u = new SpeechSynthesisUtterance(text);
  u.lang = 'hi-IN';
  u.rate = 0.95;
  const v = speechSynthesis.getVoices().find(v => v.lang === 'hi-IN');
  if (v) u.voice = v;
  speechSynthesis.speak(u);
}

function beep() {
  if (!sound) return;
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.connect(gain); gain.connect(ctx.destination);
    osc.frequency.value = 880; gain.gain.value = 0.15;
    osc.start(); osc.stop(ctx.currentTime + 0.25);
  } catch (e) { /* audio support nahi */ }
}

function ticket(o) {
  const next = NEXT[o.status];
  return `<article class="ticket st-${o.status}">
    <header><span class="tno"><small>Table</small>${o.tableNumber}</span>
      <span class="stl">${LABEL[o.status] || o.status}<small>${minutesAgo(o.createdAt)} min pehle</small></span></header>
    <ul>${o.items.map(i => `<li><b>${i.quantity}×</b> ${esc(i.name)}</li>`).join('')}</ul>
    ${o.note ? `<p class="note">${esc(o.note)}</p>` : ''}
    <footer>
      ${next ? `<button class="primary" data-id="${o.id}" data-st="${next[0]}">${next[1]}</button>` : ''}
      ${o.status === 'NEW' ? `<button class="ghost" data-id="${o.id}" data-st="CANCELLED">Cancel</button>` : ''}
    </footer></article>`;
}

function draw() {
  $('#calls').innerHTML = calls.map(c =>
    `<div class="call"><div><b>Table ${c.tableNumber}</b> ${esc(c.reasonText)}<small>${minutesAgo(c.createdAt)} min pehle</small></div>
     <button data-ack="${c.id}">Aa raha hu</button></div>`).join('');
  $('#board').innerHTML = orders.length
    ? orders.map(ticket).join('')
    : '<p class="empty">Abhi koi order nahi. Chai pi lo.</p>';
}

async function load() {
  try {
    [orders, calls] = await Promise.all([api('GET', '/api/kitchen/orders'), api('GET', '/api/kitchen/waiter-calls')]);
    draw();
  } catch (e) { toast(e.message); }
}

function connect() {
  const es = new EventSource('/api/kitchen/stream');
  es.onopen = () => { $('#conn').textContent = 'live'; $('#conn').classList.add('on'); load(); };
  es.onerror = () => { $('#conn').textContent = 'connection toot gaya, dobara jod raha hu…'; $('#conn').classList.remove('on'); };
  es.addEventListener('NEW_ORDER', e => {
    const o = JSON.parse(e.data);
    beep();
    speak(`Table ${o.tableNumber} se naya order aaya hai. ` + o.items.map(i => `${i.quantity} ${i.name}`).join(', ')
      + (o.note ? `. Note: ${o.note}` : ''));
    load();
  });
  es.addEventListener('ORDER_UPDATED', load);
  es.addEventListener('WAITER_CALL', e => {
    const c = JSON.parse(e.data);
    beep();
    speak(`Table ${c.tableNumber}. ${c.reasonText}`);
    load();
  });
  es.addEventListener('WAITER_ACK', load);
}

document.addEventListener('click', async e => {
  const b = e.target.closest('button');
  if (!b) return;
  try {
    if (b.dataset.st) { await api('PATCH', `/api/kitchen/orders/${b.dataset.id}/status`, {status: b.dataset.st}); load(); }
    else if (b.dataset.ack) { await api('POST', `/api/kitchen/waiter-calls/${b.dataset.ack}/ack`); load(); }
  } catch (err) { toast(err.message); }
});

$('#start').onclick = () => {
  sound = true;
  $('#overlay').classList.add('hidden');
  beep();
  speak('Kitchen mode shuru ho gaya');
};
$('#mute').onclick = () => {
  sound = !sound;
  $('#mute').textContent = sound ? 'Awaaz band karo' : 'Awaaz chalu karo';
  if (!sound && 'speechSynthesis' in window) speechSynthesis.cancel();
};
$('#logout').onclick = logout;

// minute wale timers refresh + jab tak waiter call open hai, 30 sec me dobara bolo
setInterval(draw, 30000);
setInterval(() => calls.forEach(c => speak(`Yaad dilana. Table ${c.tableNumber}. ${c.reasonText}`)), 30000);

connect();
