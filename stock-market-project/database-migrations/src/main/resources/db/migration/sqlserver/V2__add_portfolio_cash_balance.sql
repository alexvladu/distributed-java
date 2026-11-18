IF COL_LENGTH(N'stock_market.dbo.portfolios', N'cash_balance') IS NULL
BEGIN
    ALTER TABLE stock_market.dbo.portfolios
        ADD cash_balance DECIMAL(19, 2) NOT NULL
            CONSTRAINT portfolios_cash_balance_df DEFAULT 1000.00 WITH VALUES;
END;
GO

IF OBJECT_ID(N'stock_market.dbo.portfolios_cash_balance_ck', N'C') IS NULL
BEGIN
    ALTER TABLE stock_market.dbo.portfolios
        ADD CONSTRAINT portfolios_cash_balance_ck CHECK (cash_balance >= 0);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'portfolios_user_ix' AND object_id = OBJECT_ID(N'stock_market.dbo.portfolios')
)
BEGIN
    CREATE INDEX portfolios_user_ix ON stock_market.dbo.portfolios (user_id);
END;
GO
