INSERT INTO category (name, sort_order) VALUES
  ('Starters', 1), ('Main Course', 2), ('Breads', 3), ('Drinks', 4);

INSERT INTO menu_item (category_id, name, description, price, veg) VALUES
  ((SELECT id FROM category WHERE name='Starters'), 'Paneer Tikka', 'Tandoor me sika hua marinated paneer', 260, TRUE),
  ((SELECT id FROM category WHERE name='Starters'), 'Chicken Tikka', 'Smoky tandoori chicken, mint chutney ke saath', 320, FALSE),
  ((SELECT id FROM category WHERE name='Main Course'), 'Dal Makhani', 'Raat bhar pakayi gayi creamy dal', 240, TRUE),
  ((SELECT id FROM category WHERE name='Main Course'), 'Butter Chicken', 'Makhni gravy me tender chicken', 360, FALSE),
  ((SELECT id FROM category WHERE name='Main Course'), 'Veg Biryani', 'Khushboodar dum biryani, raita ke saath', 280, TRUE),
  ((SELECT id FROM category WHERE name='Breads'), 'Butter Naan', 'Tandoori naan, upar makhan', 60, TRUE),
  ((SELECT id FROM category WHERE name='Breads'), 'Tandoori Roti', 'Garam tandoori roti', 25, TRUE),
  ((SELECT id FROM category WHERE name='Drinks'), 'Masala Chaas', 'Thandi jeera chaas', 60, TRUE),
  ((SELECT id FROM category WHERE name='Drinks'), 'Fresh Lime Soda', 'Meetha ya namkeen', 80, TRUE);
