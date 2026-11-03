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
/** Open-ended like BrokerType: a new broker can bring new asset classes. Display only. */
export type AssetClass = 'STOCK' | 'ETF' | 'CRYPTO' | 'FX' | 'COMMODITY' | 'INDEX' | 'OTHER' | (string & {});
/** Order types we can place. Orders found at the broker can also be 'OTHER', see Order.orderType. */
export type OrderType = 'MARKET' | 'LIMIT';
/** LIVE = recorded as it happened, BACKFILL = filled in later from the broker's history. */
export type DataSource = 'LIVE' | 'BACKFILL';

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
  exchange: string; // '' = none; the same symbol can trade on several exchanges
  assetClass: AssetClass;
  side: 'LONG' | 'SHORT';
  brokerPositionId: string | null; // null on netting brokers; others can hold several positions per symbol
  quantity: string; // always positive, in units (may be fractional)
  avgPrice: string;
  marketPrice: string;
  marketValue: string;
  unrealizedPnl: string;
  currency: string;
  leverage: string | null; // null = no leverage
}

export type SignalStatus = 'EXECUTED' | 'REJECTED' | 'HOLD' | 'PENDING';

/** A provider the AI router skipped before the accepted answer, and why. */
export interface FallbackAttempt {
  provider: string;
  reason: string;
}

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
  aiProvider: string | null; // provider alias
  aiModel: string | null;
  fallbackFrom: FallbackAttempt[] | null;
}

/** UNKNOWN = outcome not known yet (e.g. a timeout): the order may have been placed. */
export type OrderStatus =
  | 'CREATED' | 'SUBMITTED' | 'PARTIALLY_FILLED' | 'FILLED' | 'CANCELLED' | 'REJECTED' | 'UNKNOWN';

/** EXTERNAL = found at the broker, not sent by us (broker's own app, stop-out, liquidation). */
export type OrderSource = 'AI' | 'MANUAL' | 'EXTERNAL';

export interface Order {
  id: number;
  createdAt: string;
  accountId: string;
  broker: BrokerType;
  source: OrderSource;
  reasoning: string | null;
  clientOrderId: string | null; // our idempotency key, null for external orders
  brokerOrderId: string | null; // ID assigned by the broker, null until acknowledged
  symbol: string;
  assetClass: AssetClass;
  side: 'BUY' | 'SELL';
  positionEffect: 'OPEN' | 'CLOSE' | null; // null on netting brokers
  brokerPositionId: string | null;
  size: OrderSize;
  orderType: OrderType | 'OTHER'; // OTHER = external orders only
  limitPrice: string | null;
  leverage: string | null; // null = no leverage
  status: OrderStatus;
  filledQuantity: string; // units
  avgFillPrice: string | null;
  commission: string; // in the order's currency
  currency: string;
  signalId: number | null;
  submittedAt: string | null;
  filledAt: string | null;
  cancelledAt: string | null;
  rejectedAt: string | null;
  closeReason: string | null;
}

/** One status change of an order. */
export interface OrderEvent {
  id: number;
  orderId: number;
  status: OrderStatus;
  eventAt: string;
  message: string | null;
}

/** One execution reported by the broker; an order can have several. */
export interface Fill {
  id: number;
  orderId: number;
  brokerFillId: string;
  quantity: string;
  price: string;
  commission: string; // negative = rebate
  commissionCurrency: string; // can differ from currency
  currency: string;
  filledAt: string;
  source: DataSource;
}

/** A round trip: entry to exit. LONG = buy then sell, SHORT = sell then buy back. */
export interface Trade {
  id: number;
  accountId: string;
  broker: BrokerType;
  symbol: string;
  assetClass: AssetClass;
  side: 'LONG' | 'SHORT';
  status: 'OPEN' | 'CLOSED';
  quantity: string;
  entryPrice: string;
  exitPrice: string | null; // null while open
  fees: string;
  realizedPnl: string | null; // after fees, null while open
  realizedPnlPct: string | null;
  successful: boolean | null; // null while open
  currency: string;
  openedAt: string;
  closedAt: string | null;
  entryReasoning: string | null;
  exitReasoning: string | null;
  entryAiProvider: string | null;
  exitAiProvider: string | null;
}

export type SnapshotReason = 'SCHEDULED' | 'AFTER_FILL' | 'ANALYSIS_RUN' | 'END_OF_DAY' | 'MANUAL';

/**
 * The account's money at one point in time. Broker history often only has the total,
 * so backfilled snapshots can have null values; show a gap, not 0.
 */
export interface AccountSnapshot {
  id: number;
  accountId: string;
  takenAt: string;
  reason: SnapshotReason;
  source: DataSource;
  currency: string;
  totalValue: string;
  investedValue: string | null;
  reservedForOrders: string | null;
  freeCash: string | null;
  cash: string | null;
  unrealizedPnl: string | null;
  realizedPnlToday: string | null;
  dailyPnl: string | null;
}

export type CashTransactionType =
  | 'DEPOSIT' | 'WITHDRAWAL' | 'DIVIDEND' | 'INTEREST' | 'FEE' | 'FINANCING' | 'TAX' | 'FX' | 'ADJUSTMENT' | 'OTHER';

/** Money moving in or out of the account other than through fills. */
export interface CashTransaction {
  id: number;
  accountId: string;
  broker: BrokerType;
  type: CashTransactionType;
  amount: string; // positive = money in, negative = money out
  currency: string;
  symbol: string | null; // e.g. the stock that paid a dividend
  description: string | null;
  occurredAt: string;
  source: DataSource;
}

/** AI provider type. Open-ended: new providers must not need a frontend change. */
export type AiProviderType = 'OPENAI' | 'GEMINI' | 'GROK' | 'CLAUDE' | 'OLLAMA' | 'OPENAI_COMPATIBLE' | (string & {});

export type AiUnavailableReason =
  | 'QUOTA_EXHAUSTED' | 'RATE_LIMITED' | 'BUDGET_REACHED' | 'AUTH_ERROR' | 'SERVER_ERROR' | 'TIMEOUT' | 'CONNECTION_ERROR';

export interface AiUsage {
  tokens: number;
  requests: number;
  cost: string;
}

/** One configured AI model: priority, availability, usage and budgets. API keys are never sent here. */
export interface AiProviderStatus {
  id: string; // alias
  type: AiProviderType;
  model: string;
  tier: 'FREE' | 'PAID';
  enabled: boolean;
  priority: number; // 1 = most preferred
  available: boolean;
  unavailableReason: AiUnavailableReason | null;
  unavailableUntil: string | null; // when it can be used again, e.g. the quota reset time
  usageToday: AiUsage;
  usageThisMonth: AiUsage;
  // Budgets: null = no limit
  maxTokensPerDay: number | null;
  maxTokensPerMonth: number | null;
  maxRequestsPerDay: number | null;
  maxCostPerMonth: string | null;
  costCurrency: string;
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
