// In-memory fake backend so the UI can be developed before the Spring Boot API exists.
import type { AccountSummary, Order, Position, RiskSettings, Signal, SystemStatus } from './types';

const minutesAgo = (m: number) => new Date(Date.now() - m * 60_000).toISOString();
const delay = <T,>(value: T) => new Promise<T>((r) => setTimeout(() => r(structuredClone(value)), 200));

let status: SystemStatus = {
  tradingMode: 'PAPER',
  brokerConnected: true,
  tradingEnabled: true,
  marketOpen: true,
  lastRunAt: minutesAgo(3),
};

const account: AccountSummary = {
  netLiquidation: '102450.32',
  cash: '61210.10',
  dailyPnl: '-184.55',
  unrealizedPnl: '1240.22',
  currency: 'USD',
};

const positions: Position[] = [
  { symbol: 'AAPL', quantity: '50', avgCost: '221.40', marketPrice: '229.15', marketValue: '11457.50', unrealizedPnl: '387.50' },
  { symbol: 'MSFT', quantity: '30', avgCost: '415.00', marketPrice: '432.80', marketValue: '12984.00', unrealizedPnl: '534.00' },
  { symbol: 'NVDA', quantity: '100', avgCost: '170.10', marketPrice: '173.29', marketValue: '17329.00', unrealizedPnl: '318.72' },
];

const signals: Signal[] = [
  { id: 105, createdAt: minutesAgo(3), symbol: 'NVDA', action: 'BUY', quantity: '20', confidence: 0.58, reasoning: 'Momentum continues but RSI near overbought.', status: 'REJECTED', rejectionReason: 'Max position value exceeded (20000)' },
  { id: 104, createdAt: minutesAgo(18), symbol: 'TSLA', action: 'SELL', quantity: '10', confidence: 0.71, reasoning: 'Breakdown below 50-day moving average.', status: 'REJECTED', rejectionReason: 'Symbol not in allowed list' },
  { id: 103, createdAt: minutesAgo(33), symbol: 'MSFT', action: 'HOLD', quantity: null, confidence: 0.64, reasoning: 'Consolidating; no clear setup.', status: 'HOLD', rejectionReason: null },
  { id: 102, createdAt: minutesAgo(48), symbol: 'NVDA', action: 'BUY', quantity: '100', confidence: 0.77, reasoning: 'Strong earnings guidance, volume breakout.', status: 'EXECUTED', rejectionReason: null },
  { id: 101, createdAt: minutesAgo(95), symbol: 'AAPL', action: 'BUY', quantity: '50', confidence: 0.69, reasoning: 'Bounce off support with rising volume.', status: 'EXECUTED', rejectionReason: null },
];

const orders: Order[] = [
  { id: 12, createdAt: minutesAgo(48), symbol: 'NVDA', side: 'BUY', quantity: '100', orderType: 'MKT', limitPrice: null, status: 'FILLED', filledQuantity: '100', avgFillPrice: '170.10', signalId: 102 },
  { id: 11, createdAt: minutesAgo(95), symbol: 'AAPL', side: 'BUY', quantity: '50', orderType: 'LMT', limitPrice: '221.50', status: 'FILLED', filledQuantity: '50', avgFillPrice: '221.40', signalId: 101 },
  { id: 10, createdAt: minutesAgo(1440), symbol: 'AMD', side: 'SELL', quantity: '40', orderType: 'LMT', limitPrice: '160.00', status: 'CANCELLED', filledQuantity: '0', avgFillPrice: null, signalId: null },
];

let settings: RiskSettings = {
  maxPositionValue: '20000',
  maxOrderValue: '5000',
  maxDailyLoss: '1000',
  maxTradesPerDay: 10,
  allowedSymbols: ['AAPL', 'MSFT', 'NVDA', 'AMD', 'GOOGL'],
  analysisIntervalMinutes: 15,
};

export const api = {
  getStatus: () => delay(status),
  getAccount: () => delay(account),
  getPositions: () => delay(positions),
  getSignals: (limit = 50) => delay(signals.slice(0, limit)),
  getOrders: (limit = 50) => delay(orders.slice(0, limit)),
  getSettings: () => delay(settings),
  saveSettings: (s: RiskSettings) => {
    settings = structuredClone(s);
    return delay(settings);
  },
  stopTrading: async () => {
    status = { ...status, tradingEnabled: false };
    await delay(null);
  },
  startTrading: async () => {
    status = { ...status, tradingEnabled: true };
    await delay(null);
  },
};
