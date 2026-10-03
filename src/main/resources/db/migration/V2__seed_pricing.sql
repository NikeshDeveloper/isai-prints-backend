INSERT INTO pricing (paper_size, color_mode, unit_price)
VALUES
    ('A4', 'BW', 5.00),
    ('A4', 'COLOR', 10.00),
    ('A5', 'BW', 3.00),
    ('A5', 'COLOR', 8.00)
ON CONFLICT ON CONSTRAINT uq_pricing_paper_color DO NOTHING;
