CREATE TABLE IF NOT EXISTS pro_plan_configs (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    amount BIGINT NOT NULL CHECK (amount > 0),
    duration_days INTEGER CHECK (duration_days IS NULL OR duration_days > 0),
    benefits TEXT,
    special_benefits TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO pro_plan_configs
    (code, name, description, amount, duration_days, benefits, special_benefits, status, featured, sort_order)
VALUES
    ('MONTHLY', 'PRO 1 month', 'Monthly PRO access', 69000, 30,
     'Unlimited access to PRO lessons
Unlock every PRO vocabulary deck',
     'Priority access to new PRO content', 'ACTIVE', FALSE, 1),
    ('QUARTERLY', 'PRO 3 months', 'Quarterly PRO access', 169000, 90,
     'Unlimited access to PRO lessons
Unlock every PRO vocabulary deck',
     'Best short-term saving for regular learners', 'ACTIVE', FALSE, 2),
    ('YEARLY', 'PRO 1 year', 'Yearly PRO access', 499000, 365,
     'Unlimited access to PRO lessons
Unlock every PRO vocabulary deck',
     'Featured plan for committed learners', 'ACTIVE', TRUE, 3),
    ('LIFETIME', 'PRO lifetime', 'Lifetime PRO access', 1849000, NULL,
     'Unlimited access to PRO lessons
Unlock every PRO vocabulary deck',
     'Lifetime access with one payment', 'ACTIVE', FALSE, 4)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = COALESCE(pro_plan_configs.description, EXCLUDED.description),
    amount = EXCLUDED.amount,
    duration_days = EXCLUDED.duration_days,
    benefits = COALESCE(pro_plan_configs.benefits, EXCLUDED.benefits),
    special_benefits = COALESCE(pro_plan_configs.special_benefits, EXCLUDED.special_benefits),
    status = COALESCE(pro_plan_configs.status, EXCLUDED.status),
    featured = EXCLUDED.featured,
    sort_order = EXCLUDED.sort_order,
    updated_at = NOW();
