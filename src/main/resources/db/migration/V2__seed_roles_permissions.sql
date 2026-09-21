-- V2: seed roles USER/MANAGER/ADMIN + base permissions. Idempotent.

INSERT INTO permissions (name, description) VALUES
    ('USER_READ', 'Read users/profile'),
    ('USER_WRITE', 'Create/update users'),
    ('USER_DELETE', 'Delete users'),
    ('ROLE_ASSIGN', 'Assign roles to users'),
    ('USER_ACTIVATE', 'Activate/deactivate users')
ON CONFLICT (name) DO NOTHING;

INSERT INTO roles (name, description, created_at) VALUES
    ('USER', 'Regular user: catalog, own orders, addresses, profile', NOW()),
    ('MANAGER', 'Manager: USER rights + orders of others, stats, inventory', NOW()),
    ('ADMIN', 'Administrator: full control over users, roles, catalog, orders', NOW())
ON CONFLICT (name) DO NOTHING;

-- role -> permissions mapping (matches RoleServiceImpl.DEFAULT_PERMISSIONS)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name = 'USER_READ' WHERE r.name = 'USER'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'MANAGER' AND p.name IN ('USER_READ', 'USER_WRITE', 'USER_ACTIVATE')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('USER_READ', 'USER_WRITE', 'USER_DELETE', 'ROLE_ASSIGN', 'USER_ACTIVATE')
ON CONFLICT DO NOTHING;
