# Hotel QR Order v2 (Spring Boot 3 + PostgreSQL)

## Features
- Har table ka alag, random-token QR (leak ho to "Naya QR banao")
- Customer: menu, search, veg filter, cart, live order status, "Mera bill", waiter call
- Kitchen screen: table-wise tickets + awaaz (voice) alerts
- Waiter screen: waiter calls, "taiyaar order" alert, bill + payment (Cash/UPI/Card), receipt print
- Admin: tables/QR, categories, menu (photo upload, edit), reports, staff, GST/service settings
- Security: BCrypt login, 3 roles, CSRF, login lockout, rate limits, CSP headers, server-side pricing

## Chalane ka tareeka (VS Code)
1. Database (koi ek):
   - Docker hai to:  docker compose up -d db
   - Ya pgAdmin me:  CREATE DATABASE hotel_qr;
2. .vscode/launch.json me DB_PASSWORD apna daalo
3. QrorderApplication.java me "Run" dabao (Maven alag se install karna zaroori nahi)
4. Console me admin / kitchen / waiter ke passwords ek baar print honge, note kar lo
5. http://localhost:8080 -> admin login

## Pages
/t/{token} customer | /kitchen.html | /waiter.html | /admin.html | /receipt.html?id=N

## Production
- PUBLIC_BASE_URL me apna https domain, HTTPS lagao (nginx/Caddy)
- Strong ADMIN_PASSWORD / KITCHEN_PASSWORD / WAITER_PASSWORD env me
- docker compose up -d --build
