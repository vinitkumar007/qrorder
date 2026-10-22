const token = location.pathname.split('/')[2] || '';
const base = '/api/public/t/' + encodeURIComponent(token);
const KEY = 'qr_order_' + token;
const items = new Map();   // id -> item
const cart = new Map();    // id -> qty
const STEPS = [['NEW', 'Order mil gaya'], ['PREPARING', 'Rasoi me ban raha hai'],
               ['READY', 'Taiyaar hai, aa raha hai'], ['SERVED', 'Parosa gaya. Enjoy!']];
const STATUS_TEXT = {NEW: 'Mil gaya', PREPARING: 'Ban raha hai', READY: 'Taiyaar', SERVED: 'Parosa gaya'};
let allCats = [];
let query = '';
let vegOnly = false;
let pollTimer = null;

const show = id => $(id).classList.remove('hidden');
const hide = id => $(id).classList.add('hidden');

async function init() {
  try {
    const m = await api('GET', base + '/menu');
    $('#tableNo').textContent = m.tableNumber;
    $('#restName').textContent = m.restaurantName;
    document.title = m.restaurantName + ' - Table ' + m.tableNumber;
    allCats = m.categories;
    allCats.forEach(c => c.items.forEach(i => items.set(i.id, i)));
    drawMenu();
    const saved = sessionStorage.getItem(KEY);
    if (saved) track(Number(saved));
  } catch (e) {
    $('#menu').innerHTML = '<p class="empty">Ye QR abhi chal nahi raha. Kripya waiter se poochhiye.</p>';
    hide('#waiterFab'); hide('#billBtn');
  }
}

function ctrl(id) {
  const q = cart.get(id) || 0;
  return q
    ? `<button data-dec="${id}" aria-label="Kam karo">−</button><b>${q}</b><button data-inc="${id}" aria-label="Badhao">+</button>`
    : `<button class="add" data-inc="${id}">Add</button>`;
}

function card(i) {
  const dot = i.veg === true ? 'v' : i.veg === false ? 'nv' : '';
  return `<article class="dish">
    <div class="dish-txt"><span class="dot ${dot}"></span><h3>${esc(i.name)}</h3>
      <p>${esc(i.description || '')}</p><div class="price">${money(i.price)}</div></div>
    ${i.imageUrl ? `<img src="${esc(i.imageUrl)}" alt="" loading="lazy">` : '<div class="ph" aria-hidden="true"></div>'}
    <div class="qty" id="q${i.id}">${ctrl(i.id)}</div></article>`;
}

function drawMenu() {
  const cats = allCats
    .map(c => ({name: c.name, items: c.items.filter(i =>
        (!vegOnly || i.veg === true) &&
        (!query || (i.name + ' ' + (i.description || '')).toLowerCase().includes(query)))}))
    .filter(c => c.items.length);
  $('#chips').innerHTML = cats.map((c, n) => `<a href="#cat${n}">${esc(c.name)}</a>`).join('');
  $('#menu').innerHTML = cats.length
    ? cats.map((c, n) => `<section id="cat${n}"><h2>${esc(c.name)}</h2>${c.items.map(card).join('')}</section>`).join('')
    : '<p class="empty">Is naam ki koi dish nahi mili.</p>';
}

$('#search').oninput = e => { query = e.target.value.trim().toLowerCase(); drawMenu(); };
$('#vegOnly').onchange = e => { vegOnly = e.target.checked; drawMenu(); };

function totals() {
  let count = 0, sum = 0;
  cart.forEach((q, id) => { count += q; sum += q * Number(items.get(id).price); });
  return {count, sum};
}

function updateBar() {
  const {count, sum} = totals();
  $('#cartCount').textContent = count + (count === 1 ? ' item' : ' items');
  $('#cartTotal').textContent = money(sum);
  $('#cartBar').classList.toggle('hidden', count === 0);
  document.body.classList.toggle('nobar', count === 0);
}

function renderCart() {
  const {sum} = totals();
  $('#cartLines').innerHTML = [...cart].map(([id, q]) => {
    const i = items.get(id);
    return `<div class="line"><span>${esc(i.name)}<br><small class="muted">${money(i.price)}</small></span>
      <span class="qty"><button data-dec="${id}">−</button><b>${q}</b><button data-inc="${id}">+</button></span></div>`;
  }).join('');
  $('#sumTotal').textContent = money(sum);
}

function change(id, d) {
  const q = Math.max(0, Math.min(20, (cart.get(id) || 0) + d));
  if (q === 0) cart.delete(id); else cart.set(id, q);
  const el = $('#q' + id);
  if (el) el.innerHTML = ctrl(id);
  updateBar();
  if (cart.size === 0) hide('#cartSheet'); else if (!$('#cartSheet').classList.contains('hidden')) renderCart();
}

document.addEventListener('click', e => {
  const t = e.target;
  if (t.classList && t.classList.contains('sheet') && t.hasAttribute('data-close')) { t.classList.add('hidden'); return; }
  const b = t.closest('button');
  if (!b) return;
  if (b.dataset.inc) change(Number(b.dataset.inc), 1);
  else if (b.dataset.dec) change(Number(b.dataset.dec), -1);
  else if (b.dataset.reason) callWaiter(b.dataset.reason);
});

$('#openCart').onclick = () => { renderCart(); show('#cartSheet'); };
$('#waiterFab').onclick = () => show('#waiterSheet');
$('#trackClose').onclick = () => hide('#tracker');

$('#placeBtn').onclick = async () => {
  const btn = $('#placeBtn');
  btn.disabled = true;
  try {
    const body = {items: [...cart].map(([menuItemId, quantity]) => ({menuItemId, quantity})), note: $('#note').value};
    const r = await api('POST', base + '/orders', body);
    cart.clear();
    items.forEach((_, id) => { const el = $('#q' + id); if (el) el.innerHTML = ctrl(id); });
    updateBar();
    $('#note').value = '';
    hide('#cartSheet');
    sessionStorage.setItem(KEY, r.orderId);
    track(r.orderId);
  } catch (e) {
    toast(e.message);
  } finally {
    btn.disabled = false;
  }
};

function track(id) {
  show('#tracker');
  clearInterval(pollTimer);
  const tick = async () => {
    try {
      const r = await api('GET', `${base}/orders/${id}`);
      drawTrack(r.status);
      if (r.status === 'SERVED' || r.status === 'CANCELLED') { clearInterval(pollTimer); sessionStorage.removeItem(KEY); }
    } catch (e) {
      if (e.status === 404) { clearInterval(pollTimer); sessionStorage.removeItem(KEY); hide('#tracker'); }
    }
  };
  tick();
  pollTimer = setInterval(tick, 4000);
}

function drawTrack(status) {
  if (status === 'CANCELLED') {
    $('#trackBody').innerHTML = '<li>Ye order cancel ho gaya. Kripya waiter se baat karein.</li>';
    return;
  }
  const idx = STEPS.findIndex(s => s[0] === status);
  $('#trackBody').innerHTML = STEPS.map((s, n) =>
    `<li class="${n < idx ? 'done' : n === idx ? 'now' : ''}">${s[1]}</li>`).join('');
}

$('#billBtn').onclick = async () => {
  try {
    const b = await api('GET', base + '/bill');
    if (!b.orders.length) {
      $('#billBody').innerHTML = '<p class="empty">Abhi koi order nahi hai.</p>';
    } else {
      $('#billBody').innerHTML = b.orders.map(o =>
        `<div class="bo"><div class="bo-h"><b>Order #${o.id}</b><span class="muted">${STATUS_TEXT[o.status] || o.status}</span></div>
          ${o.lines.map(l => `<div class="bl"><span>${l.quantity}× ${esc(l.name)}</span><span>${money2(l.amount)}</span></div>`).join('')}</div>`).join('')
        + `<div class="bl"><span>Subtotal</span><span>${money2(b.subtotal)}</span></div>`
        + (Number(b.tax) > 0 ? `<div class="bl"><span>GST (${Number(b.gstPercent)}%)</span><span>${money2(b.tax)}</span></div>` : '')
        + (Number(b.service) > 0 ? `<div class="bl"><span>Service (${Number(b.servicePercent)}%)</span><span>${money2(b.service)}</span></div>` : '')
        + `<div class="sum"><span>Total</span><span>${money2(b.total)}</span></div>`;
    }
    show('#billSheet');
  } catch (e) { toast(e.message); }
};

async function callWaiter(reason) {
  hide('#waiterSheet'); hide('#billSheet');
  try {
    const r = await api('POST', base + '/call-waiter', {reason});
    toast(r.message);
    const fab = $('#waiterFab');
    fab.disabled = true;
    let left = 60;
    const t = setInterval(() => {
      left--;
      fab.textContent = left > 0 ? `Waiter aa raha hai (${left}s)` : 'Waiter bulao';
      if (left <= 0) { clearInterval(t); fab.disabled = false; }
    }, 1000);
  } catch (e) {
    toast(e.message);
  }
}

init();
