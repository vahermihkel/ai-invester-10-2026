// Shapes returned by the backend REST API.
// Money/price/quantity values are strings: the backend serializes BigDecimal as strings
// so no precision is lost. Timestamps are ISO-8601 UTC strings.

export type TradingMode = 'PAPER' | 'LIVE';
export type Action = 'BUY' | 'SELL' | 'HOLD';

export interface SystemStatus {
  tradingMode: TradingMode;
  brokerConnected: boolean;
  tradingEnabled: boolean; // false = kill switch active
  marketOpen: boolean;
  lastRunAt: string | null;
}

export interface AccountSummary {
  netLiquidation: string;
  cash: string;
  dailyPnl: string;
  unrealizedPnl: string;
  currency: string;
}

export interface Position {
  symbol: string;
  quantity: string;
  avgCost: string;
  marketPrice: string;
  marketValue: string;
  unrealizedPnl: string;
}

export type SignalStatus = 'EXECUTED' | 'REJECTED' | 'HOLD' | 'PENDING';

export interface Signal {
  id: number;
  createdAt: string;
  symbol: string;
  action: Action;
  quantity: string | null;
  confidence: number; // 0..1
  reasoning: string;
  status: SignalStatus;
  rejectionReason: string | null;
}

export type OrderStatus = 'SUBMITTED' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'REJECTED';

export interface Order {
  id: number;
  createdAt: string;
  symbol: string;
  side: 'BUY' | 'SELL';
  quantity: string;
  orderType: 'MKT' | 'LMT';
  limitPrice: string | null;
  status: OrderStatus;
  filledQuantity: string;
  avgFillPrice: string | null;
  signalId: number | null;
}

export interface RiskSettings {
  maxPositionValue: string;
  maxOrderValue: string;
  maxDailyLoss: string;
  maxTradesPerDay: number;
  allowedSymbols: string[];
  analysisIntervalMinutes: number;
}
