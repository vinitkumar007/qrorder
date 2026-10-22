const $ = (sel, root = document) => root.querySelector(sel);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const money = n => '₹' + Number(n).toLocaleString('en-IN', {maximumFractionDigits: 0});

function cookie(name) {
  const hit = document.cookie.split('; ').find(r => r.startsWith(name + '='));
  return hit ? decodeURIComponent(hit.split('=')[1]) : '';
}

async function api(method, url, body) {
  const headers = {'Content-Type': 'application/json'};
  const xsrf = cookie('XSRF-TOKEN');
  if (xsrf) headers['X-XSRF-TOKEN'] = xsrf;
  const res = await fetch(url, {method, headers, credentials: 'same-origin',
    body: body === undefined ? undefined : JSON.stringify(body)});
  if (res.redirected && res.url.includes('login')) { location.href = '/login.html'; throw new Error('Login chahiye'); }
  const text = await res.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch (e) { data = null; }
  if (!res.ok) {
    const err = new Error((data && (data.message || data.error)) || ('Error ' + res.status));
    err.status = res.status;
    throw err;
  }
  return data;
}

let toastTimer;
function toast(msg) {
  let el = $('#toast');
  if (!el) { el = document.createElement('div'); el.id = 'toast'; document.body.appendChild(el); }
  el.textContent = msg;
  el.classList.remove('hidden');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.add('hidden'), 3200);
}

function minutesAgo(iso) {
  return Math.max(0, Math.floor((Date.now() - new Date(iso).getTime()) / 60000));
}

async function logout() {
  try { await api('POST', '/logout'); } catch (e) { /* redirect aayega */ }
  location.href = '/login.html';
}

const money2 = n => '₹' + Number(n).toLocaleString('en-IN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
