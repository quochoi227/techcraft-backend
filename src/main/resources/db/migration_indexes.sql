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

-- 5. Bảng vouchers (Mã giảm giá)
CREATE INDEX IF NOT EXISTS idx_vouchers_code ON vouchers(code);
CREATE INDEX IF NOT EXISTS idx_vouchers_active_dates ON vouchers(is_active, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_vouchers_created_at ON vouchers(created_at DESC);

-- 6. Bảng voucher_usages (Lịch sử sử dụng mã giảm giá)
CREATE INDEX IF NOT EXISTS idx_voucher_usages_user_voucher ON voucher_usages(user_id, voucher_id);
CREATE INDEX IF NOT EXISTS idx_voucher_usages_order_id ON voucher_usages(order_id);
CREATE INDEX IF NOT EXISTS idx_voucher_usages_voucher_id ON voucher_usages(voucher_id);

-- 7. Cập nhật dữ liệu cũ cho bảng orders (nếu chưa có subtotal_amount)
UPDATE orders SET subtotal_amount = total_amount WHERE subtotal_amount IS NULL;
UPDATE orders SET discount_amount = 0 WHERE discount_amount IS NULL;

-- ====================================================================
-- Kiểm tra các index vừa tạo
-- ====================================================================
SELECT tablename, indexname, indexdef
FROM pg_indexes
WHERE tablename IN ('pc_cases', 'products', 'pc_case_items', 'product_images', 'vouchers', 'voucher_usages')
ORDER BY tablename, indexname;
