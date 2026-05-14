INSERT INTO templates (id, name, thumbnail_url, type, is_premium, price) VALUES
(1, 'Professional Classic', '/1.png', 'FREE', false, 0),
(2, 'Executive Two Column', '/2.png', 'PREMIUM', true, 99),
(3, 'Modern Slate', '/3.png', 'PREMIUM', true, 149),
(4, 'Clean Minimal', '/4.png', 'FREE', false, 0),
(5, 'Corporate Blue', '/5.png', 'PREMIUM', true, 199),
(6, 'Tech Teal', '/6.png', 'PREMIUM', true, 149),
(7, 'Elegant Mono', '/7.png', 'FREE', false, 0),
(8, 'Premium Compact', '/8.png', 'PREMIUM', true, 199),
(9, 'Creative Timeline', '/9.png', 'PREMIUM', true, 249)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    thumbnail_url = VALUES(thumbnail_url),
    type = VALUES(type),
    is_premium = VALUES(is_premium),
    price = VALUES(price);

ALTER TABLE templates AUTO_INCREMENT = 10;
