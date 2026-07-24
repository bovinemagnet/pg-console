# M24 — Forecasting x-axis gap compression + off-by-one

- **Severity:** Major (correctness)
- **Area:** Forecasting
- **Locations:** `service/ForecastingService.java:100-103,290-295`

## Problem
(a) Regression x is the row index of *days that have data* (`index++`), so gaps compress the time axis and distort the slope; (b) the last training point sits at `x = size-1` but forecasting starts at `x = size+1`, extrapolating one extra day.

## Impact
Growth forecasts and `getDaysUntilThreshold`/`getDaysUntilStorageFull` are systematically wrong, worse with sampling gaps.

## Recommended fix
- Use actual elapsed time (days since epoch/first sample) as x; start forecasting at `x = size` (or the true next timestamp).

## Acceptance criteria
- [ ] Forecast for a linear series with a gap matches the true slope.
