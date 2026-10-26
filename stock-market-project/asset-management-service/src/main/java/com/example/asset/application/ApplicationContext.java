package com.example.asset.application;

import com.example.asset.boundary.messaging.StockPriceConsumer;
import com.example.asset.boundary.stream.MarketStreamHub;
import com.example.asset.control.AuthControl;
import com.example.asset.control.DashboardControl;
import com.example.asset.control.HealthControl;
import com.example.asset.control.MarketControl;
import com.example.asset.control.PortfolioControl;
import com.example.asset.control.TradingControl;
import com.example.asset.control.UserControl;
import com.example.asset.entity.auth.JdbcUserCredentialRepository;
import com.example.asset.entity.database.Database;
import com.example.asset.entity.portfolio.JdbcPortfolioRepository;
import com.example.asset.entity.portfolio.PortfolioRepository;
import com.example.asset.entity.trade.JdbcTradeRepository;
import com.example.asset.entity.trade.TradeRepository;
import com.example.asset.entity.user.JdbcUserRepository;
import com.example.asset.entity.user.UserRepository;

import jakarta.servlet.ServletContext;

/** Composition root: wires entities, controls, and long-running boundaries once per deployment. */
public final class ApplicationContext implements AutoCloseable {
    private static final String ATTRIBUTE = ApplicationContext.class.getName();

    private final Database database;
    private final StockPriceConsumer stockPriceConsumer;
    private final MarketStreamHub marketStream;
    private final MarketControl market;
    private final AuthControl auth;
    private final UserControl users;
    private final PortfolioControl portfolios;
    private final TradingControl trading;
    private final DashboardControl dashboard;
    private final HealthControl health;

    private ApplicationContext(AppConfig config) {
        database = new Database(config.postgres(), config.sqlServer(), config.oracle());
        UserRepository userRepository = new JdbcUserRepository(database.postgres());
        PortfolioRepository portfolioRepository = new JdbcPortfolioRepository(database.sqlServer());
        TradeRepository tradeRepository = new JdbcTradeRepository(database.oracle());

        market = new MarketControl();
        portfolios = new PortfolioControl(portfolioRepository, tradeRepository);
        users = new UserControl(userRepository, portfolios);
        auth = new AuthControl(userRepository, new JdbcUserCredentialRepository(database.postgres()), portfolios);
        trading = new TradingControl(portfolios, portfolioRepository, tradeRepository, market);
        dashboard = new DashboardControl(users, portfolios, tradeRepository);
        health = new HealthControl(database::ping, market);

        marketStream = new MarketStreamHub(market);
        stockPriceConsumer = new StockPriceConsumer(config.kafkaBootstrapServers(), config.kafkaTopic(), market);
    }

    public static ApplicationContext start(AppConfig config, ServletContext servletContext) {
        ApplicationContext context = new ApplicationContext(config);
        servletContext.setAttribute(ATTRIBUTE, context);
        context.stockPriceConsumer.start();
        return context;
    }

    public static ApplicationContext from(ServletContext servletContext) {
        if (servletContext.getAttribute(ATTRIBUTE) instanceof ApplicationContext context) {
            return context;
        }
        throw new IllegalStateException("Application is not initialized; check the startup log");
    }

    public MarketControl market() {
        return market;
    }

    public MarketStreamHub marketStream() {
        return marketStream;
    }

    public AuthControl auth() {
        return auth;
    }

    public UserControl users() {
        return users;
    }

    public PortfolioControl portfolios() {
        return portfolios;
    }

    public TradingControl trading() {
        return trading;
    }

    public DashboardControl dashboard() {
        return dashboard;
    }

    public HealthControl health() {
        return health;
    }

    @Override
    public void close() {
        marketStream.close();
        stockPriceConsumer.close();
        database.close();
    }
}
