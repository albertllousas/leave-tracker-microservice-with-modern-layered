CREATE TABLE annual_leave (
      id UUID PRIMARY KEY,
      employee_id UUID NOT NULL,
      year INTEGER NOT NULL,
      version BIGINT NOT NULL DEFAULT 0,
      leaves JSONB NOT NULL DEFAULT '[]',
      UNIQUE (employee_id, year)
);