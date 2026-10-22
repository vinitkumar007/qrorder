$('#printBtn').onclick = () => window.print();

(async () => {
  const id = new URLSearchParams(location.search).get('id');
  try {
    const r = await api('GET', '/api/billing/payments/' + encodeURIComponent(id));
    const b = r.bill;
    const merged = new Map();
    b.orders.forEach(o => o.lines.forEach(l => {
      const key = l.name + '|' + l.unitPrice;
      const m = merged.get(key) || {name: l.name, price: Number(l.unitPrice), qty: 0};
      m.qty += l.quantity;
      merged.set(key, m);
    }));
    const rows = [...merged.values()].map(m =>
      `<tr><td>${esc(m.name)}</td><td class="n">${m.qty}</td><td class="n">${money2(m.price)}</td><td class="n">${money2(m.price * m.qty)}</td></tr>`).join('');
    $('#rc').innerHTML = `
      <h1>${esc(r.restaurantName)}</h1>
      <p class="c">Table ${b.tableNumber} &middot; Bill #${r.id}<br>${new Date(r.paidAt).toLocaleString('en-IN')}</p>
      <table><thead><tr><th>Item</th><th class="n">Qty</th><th class="n">Rate</th><th class="n">Amt</th></tr></thead><tbody>${rows}</tbody></table>
      <div class="t"><span>Subtotal</span><span>${money2(b.subtotal)}</span></div>
      ${Number(b.tax) > 0 ? `<div class="t"><span>GST ${Number(b.gstPercent)}%</span><span>${money2(b.tax)}</span></div>` : ''}
      ${Number(b.service) > 0 ? `<div class="t"><span>Service ${Number(b.servicePercent)}%</span><span>${money2(b.service)}</span></div>` : ''}
      <div class="t big"><span>Total</span><span>${money2(b.total)}</span></div>
      <p class="c">Payment: ${esc(r.method)} &middot; ${esc(r.cashier)}<br>Dhanyavaad, phir aaiyega!</p>`;
  } catch (e) {
    $('#rc').textContent = e.message;
  }
})();
