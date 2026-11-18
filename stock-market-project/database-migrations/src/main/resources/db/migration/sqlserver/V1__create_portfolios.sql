IF OBJECT_ID(N'stock_market.dbo.portfolios', N'U') IS NULL
BEGIN
    CREATE TABLE stock_market.dbo.portfolios (
        portfolio_id BIGINT IDENTITY(1,1) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        portfolio_name NVARCHAR(120) NOT NULL,
        created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
    );
END;
GO

IF NOT EXISTS (SELECT 1 FROM stock_market.dbo.portfolios WHERE portfolio_name = N'Demo portfolio')
BEGIN
    INSERT INTO stock_market.dbo.portfolios (user_id, portfolio_name)
    VALUES (1, N'Demo portfolio');
END;
GO
