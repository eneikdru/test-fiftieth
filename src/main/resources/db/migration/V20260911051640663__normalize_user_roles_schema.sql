CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

INSERT INTO roles (name, description) VALUES
    ('ADMIN', 'Системный администратор'),
    ('EPIDEMIOLOGIST', 'Эпидемиолог'),
    ('RESEARCHER', 'Исследователь'),
    ('USER', 'Пользователь')
ON CONFLICT (name) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON UPPER(u.role) = r.name
ON CONFLICT DO NOTHING;
