// Shapes returned by the backend REST API.
// Money/price/quantity values are strings: the backend serializes BigDecimal as strings
// so no precision is lost. Timestamps are ISO-8601 UTC strings.
//
// All models are broker-neutral: the same shapes are used for IBKR, eToro and any broker
// added later. Never add broker-specific fields here; express differences through
// BrokerCapabilities instead.

/** Broker type, e.g. 'IBKR' or 'ETORO'. Open-ended: new brokers must not need a frontend change. */
export type BrokerType = 'IBKR' | 'ETORO' | (string & {});

/** PAPER = any broker's paper/demo/sandbox environment (IBKR paper, eToro demo/virtual). */
export type TradingMode = 'PAPER' | 'LIVE';
export type Action = 'BUY' | 'SELL' | 'HOLD';
export type AssetClass = 'STOCK' | 'ETF' | 'CRYPTO' | 'FX' | 'COMMODITY' | 'INDEX' | 'OTHER';
export type OrderType = 'MARKET' | 'LIMIT';

/**
 * How an order/signal is sized. UNITS = number of shares/coins (e.g. IBKR),
 * AMOUNT = cash amount in the account currency (e.g. eToro).
 */
export type SizeType = 'UNITS' | 'AMOUNT';

export interface OrderSize {
  type: SizeType;
  value: string;
}

/** What a broker adapter supports. The UI uses this instead of checking the broker name. */
export interface BrokerCapabilities {
  orderTypes: OrderType[];
  sizeTypes: SizeType[];
  fractionalUnits: boolean;
  shortSelling: boolean;
  leverage: boolean;
  assetClasses: AssetClass[];
}

/**
 * A configured broker account. `accountId` is our internal alias, never the real broker account number.
 * Several accounts can be configured, but only one is active (traded and shown) at a time.
 */
export interface BrokerAccount {
  accountId: string;
  broker: BrokerType;
  displayName: string;
  tradingMode: TradingMode;
  enabled: boolean; // false = configured but can't be made active
  baseCurrency: string;
  capabilities: BrokerCapabilities;
}

/** Status of the system and of the active account. */
export interface SystemStatus {
  activeAccountId: string | null; // null = no active account configured, nothing is traded
  tradingEnabled: boolean; // false = kill switch active
  brokerConnected: boolean; // connection to the active account's broker
  marketOpen: boolean;
  lastRunAt: string | null;
}

export interface AccountSummary {
  accountId: string;
  broker: BrokerType;
  equity: string; // total account value (IBKR net liquidation, eToro equity)
  cash: string;
  dailyPnl: string;
  unrealizedPnl: string;
  currency: string;
}

export interface Position {
  accountId: string;
  broker: BrokerType;
  symbol: string;
  assetClass: AssetClass;
  side: 'LONG' | 'SHORT';
  quantity: string; // always positive, in units (may be fractional)
  avgPrice: string;
  marketPrice: string;
  marketValue: string;
  unrealizedPnl: string;
  currency: string;
  leverage: string | null; // null = no leverage
}

export type SignalStatus = 'EXECUTED' | 'REJECTED' | 'HOLD' | 'PENDING';

export interface Signal {
  id: number;
  createdAt: string;
  accountId: string;
  broker: BrokerType;
  symbol: string;
  action: Action;
  size: OrderSize | null; // null for HOLD or when the AI gave no size hint
  confidence: number; // 0..1
  reasoning: string;
  status: SignalStatus;
  rejectionReason: string | null;
}

export type OrderStatus = 'SUBMITTED' | 'FILLED' | 'PARTIALLY_FILLED' | 'CANCELLED' | 'REJECTED';

export interface Order {
  id: number;
  createdAt: string;
  accountId: string;
  broker: BrokerType;
  clientOrderId: string; // our idempotency key
  brokerOrderId: string | null; // ID assigned by the broker, null until acknowledged
  symbol: string;
  side: 'BUY' | 'SELL';
  size: OrderSize;
  orderType: OrderType;
  limitPrice: string | null;
  status: OrderStatus;
  filledQuantity: string; // units
  avgFillPrice: string | null;
  currency: string;
  signalId: number | null;
}

export interface RiskLimits {
  maxPositionValue: string;
  maxOrderValue: string;
  maxDailyLoss: string;
  maxTradesPerDay: number;
  allowedSymbols: string[];
}

export interface AccountRiskSettings {
  accountId: string;
  limits: RiskLimits; // in the account's base currency
}

export interface RiskSettings {
  analysisIntervalMinutes: number;
  // Limits per configured account, so each broker keeps its own limits when you switch between them.
  accounts: AccountRiskSettings[];
}
