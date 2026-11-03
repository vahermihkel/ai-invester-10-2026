// In-memory fake backend so the UI can be developed before the Spring Boot API exists.
import type {
  AccountSummary, BrokerAccount, Order, Position, RiskSettings, Signal, SystemStatus,
} from './types';

const minutesAgo = (m: number) => new Date(Date.now() - m * 60_000).toISOString();
const delay = <T,>(value: T) => new Promise<T>((r) => setTimeout(() => r(structuredClone(value)), 200));
const byAccount = <T extends { accountId: string }>(items: T[], accountId: string) =>
  items.filter((x) => x.accountId === accountId);

const accounts: BrokerAccount[] = [
  {
    accountId: 'ibkr-paper',
    broker: 'IBKR',
    displayName: 'IBKR paper',
    tradingMode: 'PAPER',
    enabled: true,
    baseCurrency: 'USD',
    capabilities: {
      orderTypes: ['MARKET', 'LIMIT'],
      sizeTypes: ['UNITS'],
      fractionalUnits: false,
      shortSelling: true,
      leverage: false,
      assetClasses: ['STOCK', 'ETF', 'FX', 'INDEX'],
    },
  },
  {
    accountId: 'etoro-demo',
    broker: 'ETORO',
    displayName: 'eToro demo',
    tradingMode: 'PAPER',
    enabled: true,
    baseCurrency: 'USD',
    capabilities: {
      orderTypes: ['MARKET', 'LIMIT'],
      sizeTypes: ['AMOUNT', 'UNITS'],
      fractionalUnits: true,
      shortSelling: true,
      leverage: true,
      assetClasses: ['STOCK', 'ETF', 'CRYPTO', 'FX', 'COMMODITY', 'INDEX'],
    },
  },
];

let activeAccountId: string | null = 'ibkr-paper';
let tradingEnabled = true;
const connection: Record<string, { brokerConnected: boolean; marketOpen: boolean }> = {
  'ibkr-paper': { brokerConnected: true, marketOpen: true },
  'etoro-demo': { brokerConnected: true, marketOpen: true },
};
const lastRunAt = minutesAgo(3);

const summaries: AccountSummary[] = [
  { accountId: 'ibkr-paper', broker: 'IBKR', equity: '102450.32', cash: '61210.10', dailyPnl: '-184.55', unrealizedPnl: '1240.22', currency: 'USD' },
  { accountId: 'etoro-demo', broker: 'ETORO', equity: '100812.47', cash: '93300.00', dailyPnl: '96.20', unrealizedPnl: '312.47', currency: 'USD' },
];

const positions: Position[] = [
  { accountId: 'ibkr-paper', broker: 'IBKR', symbol: 'AAPL', exchange: 'NASDAQ', assetClass: 'STOCK', side: 'LONG', brokerPositionId: null, quantity: '50', avgPrice: '221.40', marketPrice: '229.15', marketValue: '11457.50', unrealizedPnl: '387.50', currency: 'USD', leverage: null },
  { accountId: 'ibkr-paper', broker: 'IBKR', symbol: 'MSFT', exchange: 'NASDAQ', assetClass: 'STOCK', side: 'LONG', brokerPositionId: null, quantity: '30', avgPrice: '415.00', marketPrice: '432.80', marketValue: '12984.00', unrealizedPnl: '534.00', currency: 'USD', leverage: null },
  { accountId: 'ibkr-paper', broker: 'IBKR', symbol: 'NVDA', exchange: 'NASDAQ', assetClass: 'STOCK', side: 'LONG', brokerPositionId: null, quantity: '100', avgPrice: '170.10', marketPrice: '173.29', marketValue: '17329.00', unrealizedPnl: '318.72', currency: 'USD', leverage: null },
  { accountId: 'etoro-demo', broker: 'ETORO', symbol: 'AAPL', exchange: '', assetClass: 'STOCK', side: 'LONG', brokerPositionId: '2150001', quantity: '13.0124', avgPrice: '222.90', marketPrice: '229.15', marketValue: '2981.79', unrealizedPnl: '81.33', currency: 'USD', leverage: null },
  { accountId: 'etoro-demo', broker: 'ETORO', symbol: 'BTC', exchange: '', assetClass: 'CRYPTO', side: 'LONG', brokerPositionId: '2149876', quantity: '0.0412', avgPrice: '109200.00', marketPrice: '114800.00', marketValue: '4729.76', unrealizedPnl: '231.14', currency: 'USD', leverage: null },
];

const ai = { aiProvider: 'claude-main', aiModel: 'claude-sonnet-5-5', fallbackFrom: null };

const signals: Signal[] = [
  { id: 107, createdAt: minutesAgo(3), accountId: 'etoro-demo', broker: 'ETORO', symbol: 'BTC', action: 'BUY', size: { type: 'AMOUNT', value: '2500' }, confidence: 0.55, reasoning: 'Trend intact but funding rates elevated.', status: 'REJECTED', rejectionReason: 'Max order value exceeded (2000)', aiProvider: 'gemini-free', aiModel: 'gemini-flash', fallbackFrom: [{ provider: 'claude-main', reason: 'QUOTA_EXHAUSTED' }] },
  { id: 106, createdAt: minutesAgo(3), accountId: 'ibkr-paper', broker: 'IBKR', symbol: 'NVDA', action: 'BUY', size: { type: 'UNITS', value: '20' }, confidence: 0.58, reasoning: 'Momentum continues but RSI near overbought.', status: 'REJECTED', rejectionReason: 'Max position value exceeded (20000)', ...ai },
  { id: 105, createdAt: minutesAgo(18), accountId: 'ibkr-paper', broker: 'IBKR', symbol: 'TSLA', action: 'SELL', size: { type: 'UNITS', value: '10' }, confidence: 0.71, reasoning: 'Breakdown below 50-day moving average.', status: 'REJECTED', rejectionReason: 'Symbol not in allowed list', ...ai },
  { id: 104, createdAt: minutesAgo(33), accountId: 'ibkr-paper', broker: 'IBKR', symbol: 'MSFT', action: 'HOLD', size: null, confidence: 0.64, reasoning: 'Consolidating; no clear setup.', status: 'HOLD', rejectionReason: null, ...ai },
  { id: 103, createdAt: minutesAgo(40), accountId: 'etoro-demo', broker: 'ETORO', symbol: 'AAPL', action: 'BUY', size: { type: 'AMOUNT', value: '2900' }, confidence: 0.66, reasoning: 'Bounce off support with rising volume.', status: 'EXECUTED', rejectionReason: null, ...ai },
  { id: 102, createdAt: minutesAgo(48), accountId: 'ibkr-paper', broker: 'IBKR', symbol: 'NVDA', action: 'BUY', size: { type: 'UNITS', value: '100' }, confidence: 0.77, reasoning: 'Strong earnings guidance, volume breakout.', status: 'EXECUTED', rejectionReason: null, ...ai },
  { id: 101, createdAt: minutesAgo(95), accountId: 'ibkr-paper', broker: 'IBKR', symbol: 'AAPL', action: 'BUY', size: { type: 'UNITS', value: '50' }, confidence: 0.69, reasoning: 'Bounce off support with rising volume.', status: 'EXECUTED', rejectionReason: null, ...ai },
];

const orderDefaults = {
  assetClass: 'STOCK', positionEffect: null, brokerPositionId: null, leverage: null, commission: '0',
  submittedAt: null, filledAt: null, cancelledAt: null, rejectedAt: null, closeReason: null,
};

const orders: Order[] = [
  { ...orderDefaults, id: 14, createdAt: minutesAgo(20), accountId: 'etoro-demo', broker: 'ETORO', source: 'EXTERNAL', reasoning: null, clientOrderId: null, brokerOrderId: 'close-2148800', symbol: 'TSLA', side: 'SELL', positionEffect: 'CLOSE', brokerPositionId: '2148800', size: { type: 'UNITS', value: '2.5' }, orderType: 'OTHER', limitPrice: null, status: 'FILLED', filledQuantity: '2.5', avgFillPrice: '301.20', currency: 'USD', signalId: null, filledAt: minutesAgo(20), closeReason: 'Stop-loss triggered at the broker' },
  { ...orderDefaults, id: 13, createdAt: minutesAgo(40), accountId: 'etoro-demo', broker: 'ETORO', source: 'AI', reasoning: 'Bounce off support with rising volume.', clientOrderId: 'sig-103-etoro-demo', brokerOrderId: '3318420077', symbol: 'AAPL', side: 'BUY', positionEffect: 'OPEN', brokerPositionId: '2150001', size: { type: 'AMOUNT', value: '2900' }, orderType: 'MARKET', limitPrice: null, status: 'FILLED', filledQuantity: '13.0124', avgFillPrice: '222.90', currency: 'USD', signalId: 103, submittedAt: minutesAgo(40), filledAt: minutesAgo(40) },
  { ...orderDefaults, id: 12, createdAt: minutesAgo(48), accountId: 'ibkr-paper', broker: 'IBKR', source: 'AI', reasoning: 'Strong earnings guidance, volume breakout.', clientOrderId: 'sig-102-ibkr-paper', brokerOrderId: '1042', symbol: 'NVDA', side: 'BUY', size: { type: 'UNITS', value: '100' }, orderType: 'MARKET', limitPrice: null, status: 'FILLED', filledQuantity: '100', avgFillPrice: '170.10', commission: '1.00', currency: 'USD', signalId: 102, submittedAt: minutesAgo(48), filledAt: minutesAgo(48) },
  { ...orderDefaults, id: 11, createdAt: minutesAgo(95), accountId: 'ibkr-paper', broker: 'IBKR', source: 'AI', reasoning: 'Bounce off support with rising volume.', clientOrderId: 'sig-101-ibkr-paper', brokerOrderId: '1041', symbol: 'AAPL', side: 'BUY', size: { type: 'UNITS', value: '50' }, orderType: 'LIMIT', limitPrice: '221.50', status: 'FILLED', filledQuantity: '50', avgFillPrice: '221.40', commission: '1.00', currency: 'USD', signalId: 101, submittedAt: minutesAgo(95), filledAt: minutesAgo(94) },
  { ...orderDefaults, id: 10, createdAt: minutesAgo(1440), accountId: 'ibkr-paper', broker: 'IBKR', source: 'MANUAL', reasoning: 'Taking profit before earnings.', clientOrderId: 'manual-10', brokerOrderId: '1037', symbol: 'AMD', side: 'SELL', size: { type: 'UNITS', value: '40' }, orderType: 'LIMIT', limitPrice: '160.00', status: 'CANCELLED', filledQuantity: '0', avgFillPrice: null, currency: 'USD', signalId: null, submittedAt: minutesAgo(1440), cancelledAt: minutesAgo(1400), closeReason: 'Cancelled by the user' },
];

let settings: RiskSettings = {
  analysisIntervalMinutes: 15,
  accounts: [
    {
      accountId: 'ibkr-paper',
      limits: { maxPositionValue: '20000', maxOrderValue: '5000', maxDailyLoss: '1000', maxTradesPerDay: 10, allowedSymbols: ['AAPL', 'MSFT', 'NVDA', 'AMD', 'GOOGL'] },
    },
    {
      accountId: 'etoro-demo',
      limits: { maxPositionValue: '5000', maxOrderValue: '2000', maxDailyLoss: '500', maxTradesPerDay: 5, allowedSymbols: ['AAPL', 'BTC'] },
    },
  ],
};

function systemStatus(): SystemStatus {
  const c = activeAccountId ? connection[activeAccountId] : undefined;
  return {
    activeAccountId,
    tradingEnabled,
    brokerConnected: c?.brokerConnected ?? false,
    marketOpen: c?.marketOpen ?? false,
    lastRunAt,
  };
}

export const api = {
  getAccounts: () => delay(accounts),
  getStatus: () => delay(systemStatus()),
  setActiveAccount: (accountId: string) => {
    const account = accounts.find((a) => a.accountId === accountId);
    if (!account || !account.enabled) return Promise.reject(new Error(`400 Account ${accountId} is not available`));
    activeAccountId = accountId;
    return delay(systemStatus());
  },
  getAccount: (accountId: string) => {
    const summary = summaries.find((x) => x.accountId === accountId);
    return summary ? delay(summary) : Promise.reject(new Error(`404 Unknown account ${accountId}`));
  },
  getPositions: (accountId: string) => delay(byAccount(positions, accountId)),
  getSignals: (accountId: string, limit = 50) => delay(byAccount(signals, accountId).slice(0, limit)),
  getOrders: (accountId: string, limit = 50) => delay(byAccount(orders, accountId).slice(0, limit)),
  getSettings: () => delay(settings),
  saveSettings: (s: RiskSettings) => {
    settings = structuredClone(s);
    return delay(settings);
  },
  stopTrading: async () => {
    tradingEnabled = false;
    await delay(null);
  },
  startTrading: async () => {
    tradingEnabled = true;
    await delay(null);
  },
};
