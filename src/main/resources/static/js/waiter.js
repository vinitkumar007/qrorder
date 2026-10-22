let sound = false;
let orders = [];
let calls = [];
let bills = [];

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
    osc.frequency.value = 660; gain.gain.value = 0.15;
    osc.start(); osc.stop(ctx.currentTime + 0.25);
  } catch (e) { /* audio nahi */ }
}

function readyTicket(o) {
  return `<article class="ticket st-READY">
    <header><span class="tno"><small>Table</small>${o.tableNumber}</span>
      <span class="stl">Taiyaar<small>${minutesAgo(o.createdAt)} min pehle</small></span></header>
    <ul>${o.items.map(i => `<li><b>${i.quantity}×</b> ${esc(i.name)}</li>`).join('')}</ul>
    ${o.note ? `<p class="note">${esc(o.note)}</p>` : ''}
    <footer><button class="primary" data-served="${o.id}">Parosa gaya</button></footer></article>`;
}

function billCard(b) {
  const merged = new Map();
  b.orders.forEach(o => o.lines.forEach(l => {
    const m = merged.get(l.name) || {qty: 0, amount: 0};
    m.qty += l.quantity; m.amount += Number(l.amount);
    merged.set(l.name, m);
  }));
  const lines = [...merged].map(([n, m]) => `<li><b>${m.qty}×</b> ${esc(n)} <span class="r">${money2(m.amount)}</span></li>`).join('');
  const pending = b.orders.filter(o => o.status !== 'SERVED').length;
  const dis = b.allServed ? '' : 'disabled';
  return `<article class="billcard">
    <header><span class="tno"><small>Table</small>${b.tableNumber}</span>
      <span class="stl">${money2(b.total)}<small>${b.allServed ? 'Sab parosa ja chuka' : pending + ' order abhi baaki'}</small></span></header>
    <ul>${lines}</ul>
    <div class="tot"><span>Subtotal</span><span>${money2(b.subtotal)}</span></div>
    ${Number(b.tax) > 0 ? `<div class="tot"><span>GST ${Number(b.gstPercent)}%</span><span>${money2(b.tax)}</span></div>` : ''}
    ${Number(b.service) > 0 ? `<div class="tot"><span>Service ${Number(b.servicePercent)}%</span><span>${money2(b.service)}</span></div>` : ''}
    <footer>${['CASH', 'UPI', 'CARD'].map(m =>
      `<button class="primary" data-pay="${m}" data-table="${b.tableId}" ${dis}>${m} se liya</button>`).join('')}</footer></article>`;
}

function draw() {
  $('#calls').innerHTML = calls.map(c =>
    `<div class="call"><div><b>Table ${c.tableNumber}</b> ${esc(c.reasonText)}<small>${minutesAgo(c.createdAt)} min pehle</small></div>
     <button data-ack="${c.id}">Aa raha hu</button></div>`).join('');
  const ready = orders.filter(o => o.status === 'READY');
  $('#ready').innerHTML = ready.length ? ready.map(readyTicket).join('') : '<p class="empty">Abhi koi order parosne ke liye taiyaar nahi.</p>';
  $('#bills').innerHTML = bills.length ? bills.map(billCard).join('') : '<p class="empty">Koi bill baaki nahi.</p>';
}

async function load() {
  try {
    [orders, calls, bills] = await Promise.all([
      api('GET', '/api/kitchen/orders'), api('GET', '/api/kitchen/waiter-calls'), api('GET', '/api/billing/open')]);
    draw();
  } catch (e) { toast(e.message); }
}

function connect() {
  const es = new EventSource('/api/kitchen/stream');
  es.onopen = () => { $('#conn').textContent = 'live'; $('#conn').classList.add('on'); load(); };
  es.onerror = () => { $('#conn').textContent = 'connection toot gaya, dobara jod raha hu…'; $('#conn').classList.remove('on'); };
  es.addEventListener('NEW_ORDER', load);
  es.addEventListener('ORDER_UPDATED', e => {
    const o = JSON.parse(e.data);
    if (o.status === 'READY') { beep(); speak(`Table ${o.tableNumber} ka order taiyaar hai, parosna hai`); }
    load();
  });
  es.addEventListener('WAITER_CALL', e => {
    const c = JSON.parse(e.data);
    beep();
    speak(`Table ${c.tableNumber}. ${c.reasonText}`);
    load();
  });
  es.addEventListener('WAITER_ACK', load);
  es.addEventListener('BILL_PAID', load);
}

document.addEventListener('click', async e => {
  const b = e.target.closest('button');
  if (!b || b.disabled) return;
  try {
    if (b.dataset.served) {
      await api('PATCH', `/api/kitchen/orders/${b.dataset.served}/status`, {status: 'SERVED'});
    } else if (b.dataset.ack) {
      await api('POST', `/api/kitchen/waiter-calls/${b.dataset.ack}/ack`);
    } else if (b.dataset.pay) {
      if (!confirm(`Table ka bill ${b.dataset.pay} se settle karna hai?`)) return;
      const r = await api('POST', `/api/billing/table/${b.dataset.table}/pay`, {method: b.dataset.pay});
      const ban = $('#receiptBanner');
      ban.innerHTML = `Payment ho gaya (${money2(r.bill.total)}). <a href="/receipt.html?id=${r.id}" target="_blank" rel="noopener">Receipt kholo aur print karo</a>`;
      ban.classList.remove('hidden');
    } else return;
    load();
  } catch (err) { toast(err.message); }
});

$('#start').onclick = () => { sound = true; $('#overlay').classList.add('hidden'); beep(); speak('Waiter mode shuru ho gaya'); };
$('#mute').onclick = () => {
  sound = !sound;
  $('#mute').textContent = sound ? 'Awaaz band karo' : 'Awaaz chalu karo';
  if (!sound && 'speechSynthesis' in window) speechSynthesis.cancel();
};
$('#logout').onclick = logout;
setInterval(draw, 30000);
setInterval(() => calls.forEach(c => speak(`Yaad dilana. Table ${c.tableNumber}. ${c.reasonText}`)), 30000);
connect();
