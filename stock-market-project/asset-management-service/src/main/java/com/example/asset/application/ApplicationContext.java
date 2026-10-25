package com.example.asset.application;

import com.example.asset.control.*;
import com.example.asset.entity.portfolio.*;
import com.example.asset.entity.trade.*;
import com.example.asset.entity.user.*;
import com.example.asset.entity.auth.*;

public final class ApplicationContext {
    private static final UserRepository USERS = new JdbcUserRepository();
    private static final PortfolioRepository PORTFOLIOS = new JdbcPortfolioRepository();
    private static final TradeRepository TRADES = new JdbcTradeRepository();
    private static final UserCredentialRepository CREDENTIALS = new JdbcUserCredentialRepository();
    private static final UserControl USER_CONTROL = new UserControl(USERS);
    private static final PortfolioControl PORTFOLIO_CONTROL = new PortfolioControl(USERS, PORTFOLIOS);
    private static final TradeControl TRADE_CONTROL = new TradeControl(USERS, PORTFOLIOS, TRADES);
    private static final DashboardControl DASHBOARD_CONTROL = new DashboardControl(USERS, PORTFOLIOS, TRADES);
    private static final AuthControl AUTH_CONTROL = new AuthControl(USERS, CREDENTIALS);
    private ApplicationContext() {}
    public static UserControl users() { return USER_CONTROL; }
    public static PortfolioControl portfolios() { return PORTFOLIO_CONTROL; }
    public static TradeControl trades() { return TRADE_CONTROL; }
    public static DashboardControl dashboard() { return DASHBOARD_CONTROL; }
    public static AuthControl auth() { return AUTH_CONTROL; }
}
