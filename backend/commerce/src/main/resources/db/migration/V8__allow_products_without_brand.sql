-- IGDB can omit both developer and publisher; do not invent a brand in that case.
ALTER TABLE products ALTER COLUMN brand DROP NOT NULL;
