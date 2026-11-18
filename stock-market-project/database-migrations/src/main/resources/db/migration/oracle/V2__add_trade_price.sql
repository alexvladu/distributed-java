-- Execution price per share; NULL only for trades recorded before prices were tracked.
ALTER TABLE trades ADD (price NUMBER(19, 4));

CREATE INDEX trades_portfolio_ix ON trades (portfolio_id);
