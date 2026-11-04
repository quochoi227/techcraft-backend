-- ====================================================================
-- Script đánh chỉ mục (Index) tối ưu hiệu năng truy vấn cho TechCraft
-- Cơ sở dữ liệu: PostgreSQL (Neon Cloud)
-- ====================================================================

-- 1. Bảng pc_cases (Cấu hình PC Case)
CREATE INDEX IF NOT EXISTS idx_pc_cases_is_active ON pc_cases(is_active);
CREATE INDEX IF NOT EXISTS idx_pc_cases_use_case ON pc_cases(use_case);
CREATE INDEX IF NOT EXISTS idx_pc_cases_total_price ON pc_cases(total_price);
CREATE INDEX IF NOT EXISTS idx_pc_cases_created_at ON pc_cases(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_pc_cases_active_created ON pc_cases(is_active, created_at DESC);

-- 2. Bảng products (Sản phẩm / Linh kiện lẻ)
CREATE INDEX IF NOT EXISTS idx_products_is_active ON products(is_active);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_price ON products(price);
CREATE INDEX IF NOT EXISTS idx_products_stock_quantity ON products(stock_quantity);
CREATE INDEX IF NOT EXISTS idx_products_active_category ON products(is_active, category);

-- 3. Bảng pc_case_items (Liên kết PC Case và Linh kiện)
CREATE INDEX IF NOT EXISTS idx_pc_case_items_product_id ON pc_case_items(product_id);
CREATE INDEX IF NOT EXISTS idx_pc_case_items_pc_case_id ON pc_case_items(pc_case_id);

-- 4. Bảng product_images (Hình ảnh sản phẩm)
CREATE INDEX IF NOT EXISTS idx_product_images_product_id ON product_images(product_id);
CREATE INDEX IF NOT EXISTS idx_product_images_prod_primary ON product_images(product_id, is_primary);

-- ====================================================================
-- Kiểm tra các index vừa tạo
-- ====================================================================
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('pc_cases', 'products', 'pc_case_items', 'product_images')
ORDER BY tablename, indexname;
