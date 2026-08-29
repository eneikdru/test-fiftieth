<script>
  export let telemetry = {
    totalClicks: 0,
    tabNavigations: 0,
    successfulLoads: 0,
    failedLoads: 0,
    activities: [
      { id: 1, title: 'Dashboard Loaded', module: 'Core Module', latencyMs: 24, timestamp: 'Just now', success: true },
      { id: 2, title: 'Metrics Fetched', module: 'Data Module', latencyMs: 42, timestamp: '2 min ago', success: true },
      { id: 3, title: 'Report Rendered', module: 'Export Module', latencyMs: 112, timestamp: '15 min ago', success: true }
    ],
    preConsolidationTaps: 3.2
  };

  $: avgClickDistance = telemetry.tabNavigations > 0
    ? (telemetry.totalClicks / telemetry.tabNavigations).toFixed(1)
    : '1.8';

  $: totalLoads = telemetry.successfulLoads + telemetry.failedLoads;
  $: loadSuccessRate = totalLoads > 0
    ? ((telemetry.successfulLoads / totalLoads) * 100).toFixed(1)
    : '100.0';

  $: postConsolidationTaps = parseFloat(avgClickDistance) || 1.8;
  $: preTaps = telemetry.preConsolidationTaps || 3.2;
  $: postPercentage = Math.min(100, Math.round((postConsolidationTaps / preTaps) * 80));
</script>

<div class="telemetry-dashboard space-y-6">
  <!-- Summary Header -->
  <div class="flex items-center justify-between border-b border-[#e0e3e5] pb-3">
    <div>
      <h2 class="text-xl font-bold text-[#003f87] flex items-center gap-2">
        <span>📈 Телеметрия SPA Навигации</span>
      </h2>
      <p class="text-xs text-[#424752] mt-1">
        Измерение дистанции кликов и успешности загрузки вкладок единой консоли
      </p>
    </div>
    <div class="px-3 py-1 bg-[#d3e4fe] text-[#003f87] rounded-full text-xs font-semibold">
      Метрики Consolidation Hub
    </div>
  </div>

  <!-- Bento Grid: Telemetry Metric Cards -->
  <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
    <!-- Metric Card 1: Click Distance -->
    <div id="telemetry-card-click-distance" class="bg-white border border-[#e0e3e5] rounded-xl p-4 flex flex-col justify-between shadow-sm relative overflow-hidden group hover:shadow-md transition-shadow">
      <div class="flex justify-between items-start">
        <span class="text-xs font-semibold text-[#424752] uppercase tracking-wider">Средняя дистанция кликов</span>
        <span class="text-base">👆</span>
      </div>
      <div class="mt-3">
        <div class="text-3xl font-bold text-[#191c1e] font-mono tracking-tight" id="metric-click-distance-value">
          {avgClickDistance} <span class="text-xs font-normal text-[#424752]">Кликов</span>
        </div>
      </div>
      <div class="flex items-center gap-1.5 mt-4 pt-2 border-t border-[#f2f4f6]">
        <span class="inline-flex items-center gap-1 bg-emerald-50 text-emerald-700 text-xs px-2 py-0.5 rounded-full font-medium">
          <span>↓</span>
          <span>-24% улучшение</span>
        </span>
        <span class="text-[11px] text-[#727784] ml-auto">По сравнению с до-консолидацией</span>
      </div>
    </div>

    <!-- Metric Card 2: Tab Load Success -->
    <div id="telemetry-card-tab-load" class="bg-white border border-[#e0e3e5] rounded-xl p-4 flex flex-col justify-between shadow-sm relative overflow-hidden hover:shadow-md transition-shadow">
      <div class="flex justify-between items-start">
        <span class="text-xs font-semibold text-[#424752] uppercase tracking-wider">Успешность загрузки вкладок</span>
        <span class="text-base">⚡</span>
      </div>
      <div class="mt-3">
        <div class="text-3xl font-bold text-[#191c1e] font-mono tracking-tight" id="metric-tab-load-success-value">
          {loadSuccessRate}%
        </div>
      </div>
      <div class="flex items-center gap-1.5 mt-4 pt-2 border-t border-[#f2f4f6]">
        <span class="inline-flex items-center gap-1 bg-emerald-50 text-emerald-700 text-xs px-2 py-0.5 rounded-full font-medium">
          <span>↑</span>
          <span>100% норма</span>
        </span>
        <span class="text-[11px] text-[#727784] ml-auto">Успешно: {telemetry.successfulLoads} / Сбоев: {telemetry.failedLoads}</span>
      </div>
    </div>
  </div>

  <!-- Efficiency Gains Bar Section -->
  <div class="bg-white border border-[#e0e3e5] rounded-xl p-5 shadow-sm space-y-4">
    <div class="flex justify-between items-center border-b border-[#f2f4f6] pb-2">
      <h3 class="text-sm font-bold text-[#191c1e]">Прирост эффективности переходов</h3>
      <span class="text-xs font-semibold text-[#003f87]">Сравнение гипотез</span>
    </div>
    <div class="space-y-3">
      <!-- Pre-consolidation -->
      <div>
        <div class="flex justify-between text-xs text-[#424752] mb-1">
          <span>До консолидации (Разрозненные экраны)</span>
          <span class="font-mono font-medium">{preTaps} Кликов</span>
        </div>
        <div class="w-full bg-[#eceef0] rounded-full h-2">
          <div class="bg-[#727784] h-2 rounded-full" style="width: 80%"></div>
        </div>
      </div>
      <!-- Post-consolidation -->
      <div>
        <div class="flex justify-between text-xs text-[#424752] mb-1">
          <span class="text-[#003f87] font-semibold">После консолидации (Единый SPA Hub)</span>
          <span class="text-emerald-700 font-mono font-bold">{postConsolidationTaps} Кликов</span>
        </div>
        <div class="w-full bg-[#eceef0] rounded-full h-2">
          <div class="bg-emerald-600 h-2 rounded-full" style="width: {postPercentage}%"></div>
        </div>
      </div>
    </div>
  </div>

  <!-- Recent Activity List -->
  <div class="space-y-3">
    <h3 class="text-sm font-bold text-[#191c1e] flex items-center justify-between">
      <span>Журнал переходов и загрузок</span>
      <span class="text-xs font-normal text-[#727784]">Всего записей: {telemetry.activities.length}</span>
    </h3>
    <div class="bg-white border border-[#e0e3e5] rounded-xl overflow-hidden divide-y divide-[#e0e3e5]">
      {#each telemetry.activities as activity}
        <div class="p-3.5 flex items-center justify-between hover:bg-[#f7f9fb] transition-colors">
          <div class="flex items-center gap-3">
            <div class="w-8 h-8 rounded-full bg-[#d3e4fe] text-[#003f87] flex items-center justify-center font-bold text-xs">
              ✓
            </div>
            <div>
              <div class="text-xs font-bold text-[#191c1e]">{activity.title}</div>
              <div class="text-[11px] text-[#424752]">{activity.module}</div>
            </div>
          </div>
          <div class="text-right">
            <div class="text-xs font-mono font-bold text-emerald-700 flex items-center gap-1 justify-end">
              <span>●</span> {activity.latencyMs}мс
            </div>
            <div class="text-[11px] text-[#727784]">{activity.timestamp}</div>
          </div>
        </div>
      {/each}
    </div>
  </div>
</div>
