import { useMemo } from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';

const fmtMoney = (a) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(a ?? 0);

// Rút gọn trục Y: 1.500.000 → "1.5tr", 250.000 → "250k"
const fmtAxis = (v) => {
  if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(v % 1_000_000 === 0 ? 0 : 1)}tr`;
  if (v >= 1_000) return `${Math.round(v / 1_000)}k`;
  return String(v ?? 0);
};

// Rút gọn nhãn trục X theo granularity:
//   DAY:   "2026-05-31" → "31/05"
//   MONTH: "2026-05"    → "05/2026"
const fmtPeriod = (period, granularity) => {
  if (!period) return '';
  if (granularity === 'MONTH') {
    const [y, m] = period.split('-');
    return `${m}/${y}`;
  }
  const [, m, d] = period.split('-');
  return `${d}/${m}`;
};

function ChartTooltip({ active, payload, granularity }) {
  if (!active || !payload?.length) return null;
  const point = payload[0].payload;
  return (
    <div className="rounded-lg border bg-background px-3 py-2 shadow-md text-xs">
      <p className="font-medium mb-1">
        {granularity === 'MONTH' ? 'Tháng ' : 'Ngày '}
        {fmtPeriod(point.period, granularity)}
      </p>
      <p className="text-emerald-600 font-semibold">{fmtMoney(point.revenue)}</p>
      <p className="text-muted-foreground mt-0.5">{point.count} giao dịch</p>
    </div>
  );
}

/**
 * Biểu đồ cột doanh thu theo ngày/tháng.
 * @param {Array<{period, revenue, count}>} data - series từ BE
 * @param {'DAY'|'MONTH'} granularity
 */
export default function RevenueChart({ data = [], granularity = 'DAY' }) {
  const chartData = useMemo(
    () =>
      (data ?? []).map((p) => ({
        ...p,
        revenue: Number(p.revenue ?? 0),
        label: fmtPeriod(p.period, granularity),
      })),
    [data, granularity]
  );

  if (!chartData.length) {
    return (
      <div className="flex h-64 items-center justify-center rounded-xl border border-dashed text-sm text-muted-foreground">
        Chưa có dữ liệu doanh thu trong khoảng thời gian này.
      </div>
    );
  }

  return (
    <div className="h-72 w-full">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={chartData} margin={{ top: 8, right: 8, left: 8, bottom: 4 }}>
          <defs>
            <linearGradient id="revenueBar" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#10b981" stopOpacity={1} />
              <stop offset="100%" stopColor="#34d399" stopOpacity={0.75} />
            </linearGradient>
          </defs>
          <CartesianGrid strokeDasharray="3 3" vertical={false} className="stroke-muted" />
          <XAxis
            dataKey="label"
            tick={{ fontSize: 11 }}
            tickLine={false}
            axisLine={false}
            interval="preserveStartEnd"
            minTickGap={16}
          />
          <YAxis
            tickFormatter={fmtAxis}
            tick={{ fontSize: 11 }}
            tickLine={false}
            axisLine={false}
            width={44}
          />
          <Tooltip
            cursor={{ fill: 'rgba(16,185,129,0.08)' }}
            content={<ChartTooltip granularity={granularity} />}
          />
          <Bar dataKey="revenue" fill="url(#revenueBar)" radius={[6, 6, 0, 0]} maxBarSize={48} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
