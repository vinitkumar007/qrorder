const TABS = ['Tables', 'Menu', 'Reports', 'Staff', 'Settings'];
let menuItems = [];
let editingId = null;

function tab(name) {
  TABS.forEach(t => {
    $('#panel' + t).classList.toggle('hidden', t !== name);
    $('#tab' + t).classList.toggle('on', t === name);
  });
  if (name === 'Menu') { loadCats(); loadMenu(); }
  if (name === 'Reports') loadReports();
  if (name === 'Staff') loadStaff();
  if (name === 'Settings') loadSettings();
}
TABS.forEach(t => { $('#tab' + t).onclick = () => tab(t); });
$('#logout').onclick = logout;
$('#printBtn').onclick = () => window.print();

// ---------------- tables ----------------
async function loadTables() {
  const list = await api('GET', '/api/admin/tables');
  $('#tables').innerHTML = list.length ? list.map(t => `
    <article class="qrcard ${t.active ? '' : 'off'}">
      <div class="plate sm"><small>Table</small><b>${t.tableNumber}</b></div>
      <img src="/api/admin/tables/${t.id}/qr.png?v=${Date.now()}" alt="QR for table ${t.tableNumber}">
      <p class="url">${esc(t.url)}</p>
      <div class="row noprint">
        <button class="ghost" data-rotate="${t.id}">Naya QR banao</button>
        <button class="ghost" data-ttoggle="${t.id}">${t.active ? 'Band karo' : 'Chalu karo'}</button>
      </div>
    </article>`).join('') : '<p class="empty">Pehla table jodo, uska QR yahin dikhega.</p>';
}

$('#tableForm').onsubmit = async e => {
  e.preventDefault();
  try {
    await api('POST', '/api/admin/tables', {tableNumber: Number($('#tableNumber').value)});
    $('#tableNumber').value = '';
    loadTables();
  } catch (err) { toast(err.message); }
};

// ---------------- categories ----------------
async function loadCats() {
  const cats = await api('GET', '/api/admin/categories');
  const cur = $('#mCat').value;
  $('#mCat').innerHTML = cats.map(c => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
  if (cur) $('#mCat').value = cur;
  $('#cats').innerHTML = cats.map(c =>
    `<span class="chip">${esc(c.name)} <button data-cdel="${c.id}" aria-label="${esc(c.name)} hatao">×</button></span>`).join('');
}

$('#catForm').onsubmit = async e => {
  e.preventDefault();
  try {
    await api('POST', '/api/admin/categories', {name: $('#catName').value});
    $('#catName').value = '';
    loadCats();
  } catch (err) { toast(err.message); }
};

// ---------------- menu ----------------
async function loadMenu() {
  menuItems = await api('GET', '/api/admin/menu');
  $('#menuRows').innerHTML = menuItems.map(m => `
    <tr class="${m.available ? '' : 'off-row'}">
      <td>${esc(m.name)}${m.veg === false ? ' <small class="muted">(non-veg)</small>' : ''}</td>
      <td>${esc(m.category)}</td><td>${money(m.price)}</td>
      <td class="acts">
        <button class="ghost" data-medit="${m.id}">Edit</button>
        <button class="ghost" data-mtoggle="${m.id}">${m.available ? 'Khatam' : 'Wapas chalu'}</button>
        <button class="ghost" data-mdel="${m.id}">Delete</button></td>
    </tr>`).join('');
}

function resetMenuForm() {
  editingId = null;
  $('#menuForm').reset();
  $('#mPreview').classList.add('hidden');
  $('#mSubmit').textContent = 'Dish jodo';
  $('#formTitle').textContent = 'Nayi dish';
  $('#mCancel').classList.add('hidden');
}

function startEdit(id) {
  const m = menuItems.find(x => x.id === id);
  if (!m) return;
  editingId = id;
  $('#mCat').value = m.categoryId;
  $('#mName').value = m.name;
  $('#mDesc').value = m.description || '';
  $('#mPrice').value = m.price;
  $('#mVeg').value = m.veg === false ? 'false' : 'true';
  $('#mImg').value = m.imageUrl || '';
  if (m.imageUrl) { $('#mPreview').src = m.imageUrl; $('#mPreview').classList.remove('hidden'); }
  else $('#mPreview').classList.add('hidden');
  $('#mSubmit').textContent = 'Update karo';
  $('#formTitle').textContent = 'Dish edit: ' + m.name;
  $('#mCancel').classList.remove('hidden');
  $('#menuForm').scrollIntoView({behavior: 'smooth', block: 'center'});
}

$('#mCancel').onclick = resetMenuForm;

$('#mFile').onchange = async e => {
  const f = e.target.files[0];
  if (!f) return;
  const fd = new FormData();
  fd.append('file', f);
  try {
    const res = await fetch('/api/admin/uploads', {method: 'POST', body: fd, credentials: 'same-origin',
      headers: {'X-XSRF-TOKEN': cookie('XSRF-TOKEN')}});
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.message || 'Upload nahi hua');
    $('#mImg').value = data.url;
    $('#mPreview').src = data.url;
    $('#mPreview').classList.remove('hidden');
    toast('Photo upload ho gayi');
  } catch (err) { toast(err.message); }
  e.target.value = '';
};

$('#menuForm').onsubmit = async e => {
  e.preventDefault();
  const body = {
    categoryId: Number($('#mCat').value), name: $('#mName').value, description: $('#mDesc').value,
    price: Number($('#mPrice').value), imageUrl: $('#mImg').value, veg: $('#mVeg').value === 'true'};
  try {
    if (editingId) await api('PUT', '/api/admin/menu/' + editingId, body);
    else await api('POST', '/api/admin/menu', body);
    resetMenuForm();
    loadMenu();
  } catch (err) { toast(err.message); }
};

// ---------------- reports ----------------
function bars(rows, label, value, fmt) {
  const max = Math.max(1, ...rows.map(r => Number(r[value])));
  return rows.length ? rows.map(r =>
    `<div class="bar"><span>${esc(r[label])}</span><i style="width:${Math.max(2, Number(r[value]) / max * 100)}%"></i><b>${fmt(r[value])}</b></div>`).join('')
    : '<p class="muted">Abhi data nahi hai.</p>';
}

async function loadReports() {
  try {
    const r = await api('GET', '/api/admin/reports?days=' + $('#repDays').value);
    $('#reports').innerHTML = `
      <div class="stats">
        <div class="stat"><small>Aaj ke orders</small><b>${r.today.orders}</b></div>
        <div class="stat"><small>Aaj ki bikri (tax se pehle)</small><b>${money(r.today.sales)}</b></div>
        <div class="stat"><small>Aaj settle hue bills</small><b>${r.paidToday.bills}</b></div>
        <div class="stat"><small>Aaj mila paisa</small><b>${money2(r.paidToday.amount)}</b></div>
      </div>
      <h2 class="h2">Payment ka tareeka (aaj)</h2>${bars(r.byMethod, 'method', 'amount', money2)}
      <h2 class="h2">Din ke hisab se bikri</h2>${bars(r.daily, 'd', 'sales', money)}
      <h2 class="h2">Sabse zyada bikne wali dishes</h2>${bars(r.topItems, 'name', 'qty', v => v + ' plate')}
      <h2 class="h2">Aaj ghante ke hisab se orders</h2>${bars(r.hourly.map(h => ({hr: h.hr + ':00', orders: h.orders})), 'hr', 'orders', v => v)}`;
  } catch (err) { toast(err.message); }
}
$('#repDays').onchange = loadReports;
$('#repReload').onclick = loadReports;

// ---------------- staff ----------------
async function loadStaff() {
  const list = await api('GET', '/api/admin/staff');
  $('#staffRows').innerHTML = list.map(u => `
    <tr><td>${esc(u.username)}</td><td>${esc(u.role)}</td>
    <td class="acts"><button class="ghost" data-spass="${u.id}">Password reset</button>
      <button class="ghost" data-sdel="${u.id}">Delete</button></td></tr>`).join('');
}

$('#staffForm').onsubmit = async e => {
  e.preventDefault();
  try {
    await api('POST', '/api/admin/staff', {username: $('#sUser').value, password: $('#sPass').value, role: $('#sRole').value});
    e.target.reset();
    loadStaff();
  } catch (err) { toast(err.message); }
};

$('#pwForm').onsubmit = async e => {
  e.preventDefault();
  try {
    const r = await api('POST', '/api/account/password', {current: $('#pwCur').value, newPassword: $('#pwNew').value});
    toast(r.message);
    e.target.reset();
  } catch (err) { toast(err.message); }
};

// ---------------- settings ----------------
async function loadSettings() {
  const s = await api('GET', '/api/admin/settings');
  $('#setName').value = s.restaurantName;
  $('#setGst').value = s.gstPercent;
  $('#setSvc').value = s.servicePercent;
}

$('#setForm').onsubmit = async e => {
  e.preventDefault();
  try {
    await api('PUT', '/api/admin/settings', {restaurantName: $('#setName').value,
      gstPercent: Number($('#setGst').value), servicePercent: Number($('#setSvc').value)});
    toast('Settings save ho gayi');
  } catch (err) { toast(err.message); }
};

// ---------------- common clicks ----------------
document.addEventListener('click', async e => {
  const b = e.target.closest('button');
  if (!b) return;
  const d = b.dataset;
  try {
    if (d.rotate) {
      if (!confirm('Naya QR banega aur purana QR turant kaam karna band kar dega. Pakka?')) return;
      await api('POST', `/api/admin/tables/${d.rotate}/rotate`); loadTables();
    } else if (d.ttoggle) {
      await api('POST', `/api/admin/tables/${d.ttoggle}/toggle`); loadTables();
    } else if (d.cdel) {
      if (!confirm('Is category ko hatana hai?')) return;
      await api('DELETE', `/api/admin/categories/${d.cdel}`); loadCats();
    } else if (d.medit) {
      startEdit(Number(d.medit));
    } else if (d.mtoggle) {
      await api('POST', `/api/admin/menu/${d.mtoggle}/toggle`); loadMenu();
    } else if (d.mdel) {
      if (!confirm('Is dish ko delete karna hai?')) return;
      await api('DELETE', `/api/admin/menu/${d.mdel}`); loadMenu();
    } else if (d.spass) {
      const pw = prompt('Naya password (kam se kam 8 akshar):');
      if (!pw) return;
      const r = await api('POST', `/api/admin/staff/${d.spass}/password`, {password: pw});
      toast(r.message);
    } else if (d.sdel) {
      if (!confirm('Is user ko delete karna hai?')) return;
      await api('DELETE', `/api/admin/staff/${d.sdel}`); loadStaff();
    }
  } catch (err) { toast(err.message); }
});

loadTables();
