<script>
  import { createEventDispatcher } from 'svelte';
  import CatalogSearch from './CatalogSearch.svelte';
  import DossierSearch from './DossierSearch.svelte';
  import PrivacySettings from './PrivacySettings.svelte';

  const dispatch = createEventDispatcher();

  export let activeTab = 'catalog'; // 'catalog' | 'dossier' | 'foci' | 'privacy'
  export let currentUser = {
    id: 'usr_101',
    username: 'admin_user',
    role: 'ADMIN',
    full_name: 'Иванов И.И. (Администратор)',
    email: 'ivanov@epidemiology-inst.ru'
  };

  const tabs = [
    { id: 'tab-catalog', key: 'catalog', title: 'Каталог протоколов', icon: '📁' },
    { id: 'tab-dossier', key: 'dossier', title: 'Аналитика досье', icon: '📊' },
    { id: 'tab-foci', key: 'foci', title: 'Категоризация очагов', icon: '🔍' },
    { id: 'tab-privacy', key: 'privacy', title: 'Безопасность & GDPR', icon: '🛡️' }
  ];

  function switchTab(tabKey) {
    activeTab = tabKey;
    dispatch('tabChange', { tab: tabKey });
  }

  // RootCause / Outbreaks categorisation state
  let fociQuery = '';
  let selectedCategory = 'ALL';
  let fociResults = [
    { id: 'FOC-001', location: 'г. Москва, ВАО', hazardLevel: 'HIGH', category: 'Пищевой очаг (Сальмонеллез)', status: 'ACTIVE', cases: 14 },
    { id: 'FOC-002', location: 'Московская обл., г. Подольск', hazardLevel: 'MEDIUM', category: 'Респираторный очаг (ОРВИ/Грипп)', status: 'MONITORING', cases: 28 },
    { id: 'FOC-003', location: 'г. Санкт-Петербург, Невский р-н', hazardLevel: 'LOW', category: 'Контактно-бытовой очаг', status: 'CLOSED', cases: 3 }
  ];

  function handleCategorizeFoci() {
    if (!fociQuery.trim()) return;
    fociResults = fociResults.filter(f =>
      f.location.toLowerCase().includes(fociQuery.toLowerCase()) ||
      f.category.toLowerCase().includes(fociQuery.toLowerCase()) ||
      f.id.toLowerCase().includes(fociQuery.toLowerCase())
    );
  }
</script>

<div class="navigation-hub-shell min-h-screen bg-[#f7f9fb] font-sans text-[#191c1e]">
  <!-- Unified Top Navigation Header -->
  <header class="bg-[#003f87] text-white shadow-md sticky top-0 z-40">
    <div class="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex items-center justify-between h-16 border-b border-[#002b5e]">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-white/10 flex items-center justify-center text-xl font-bold">
            ☣️
          </div>
          <div>
            <h1 class="text-base font-bold leading-tight">База знаний по эпидемиологии</h1>
            <p class="text-[11px] text-white/80">Единая консоль исследования и безопасности</p>
          </div>
        </div>

        <!-- User Profile Pill -->
        <div class="flex items-center gap-3 bg-white/10 px-3 py-1.5 rounded-lg border border-white/20">
          <div class="w-8 h-8 rounded-full bg-white text-[#003f87] flex items-center justify-center font-bold text-xs">
            {(currentUser?.full_name || currentUser?.username || 'U')[0]}
          </div>
          <div class="hidden sm:block text-left">
            <div class="text-xs font-semibold leading-tight">{currentUser?.full_name || currentUser?.username}</div>
            <div class="text-[10px] text-white/80">{currentUser?.role || 'ПОЛЬЗОВАТЕЛЬ'}</div>
          </div>
        </div>
      </div>

      <!-- Navigation Tabs Bar -->
      <nav class="flex space-x-1 sm:space-x-2 overflow-x-auto py-2" aria-label="Модули системы">
        {#each tabs as tab}
          <button
            id={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.key}
            aria-controls="panel-{tab.key}"
            on:click={() => switchTab(tab.key)}
            class="min-h-[44px] min-w-[44px] px-4 py-2.5 rounded-lg text-xs font-semibold transition-all flex items-center gap-2 whitespace-nowrap focus:outline-none focus:ring-2 focus:ring-white/80 {activeTab === tab.key ? 'bg-white text-[#003f87] shadow-sm font-bold' : 'text-white/90 hover:bg-white/10 hover:text-white'}"
          >
            <span class="text-sm">{tab.icon}</span>
            <span>{tab.title}</span>
          </button>
        {/each}
      </nav>
    </div>
  </header>

  <!-- Main Content Area for Active Tab Panel -->
  <main class="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-6">
    {#if activeTab === 'catalog'}
      <section id="panel-catalog" role="tabpanel" aria-labelledby="tab-catalog" class="space-y-6">
        <CatalogSearch {currentUser} />
      </section>
    {:else if activeTab === 'dossier'}
      <section id="panel-dossier" role="tabpanel" aria-labelledby="tab-dossier" class="bg-white border border-[#e0e3e5] rounded-xl p-6 shadow-sm space-y-6">
        <div class="border-b border-[#e0e3e5] pb-4">
          <h2 class="text-xl font-bold text-[#003f87]">Аналитика досье сотрудников и исследователей</h2>
          <p class="text-xs text-[#424752] mt-1">Формирование итоговых справок, приказов и выписок ученых советов</p>
        </div>
        <DossierSearch />
      </section>
    {:else if activeTab === 'foci'}
      <section id="panel-foci" role="tabpanel" aria-labelledby="tab-foci" class="bg-white border border-[#e0e3e5] rounded-xl p-6 shadow-sm space-y-6">
        <div class="border-b border-[#e0e3e5] pb-4">
          <h2 class="text-xl font-bold text-[#003f87]">Категоризация очагов и RootCauseService</h2>
          <p class="text-xs text-[#424752] mt-1">Автоматический анализ корневых причин вспышек инфекционных заболеваний</p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-12 gap-4">
          <div class="md:col-span-8">
            <label for="foci-search-input" class="block text-xs font-semibold text-[#191c1e] mb-1">Поиск очага или региона</label>
            <input
              id="foci-search-input"
              type="text"
              bind:value={fociQuery}
              placeholder="Введите локацию или тип очага..."
              class="w-full h-11 px-3.5 bg-[#f7f9fb] border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
            />
          </div>
          <div class="md:col-span-4 flex items-end">
            <button
              type="button"
              id="foci-search-btn"
              on:click={handleCategorizeFoci}
              class="w-full min-h-[44px] min-w-[44px] bg-[#003f87] text-white text-xs font-bold rounded-lg hover:bg-[#002b5e] transition-colors focus:ring-2 focus:ring-[#003f87]/50"
            >
              Анализировать очаги
            </button>
          </div>
        </div>

        <div class="overflow-x-auto border border-[#e0e3e5] rounded-lg">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-[#f7f9fb] border-b border-[#e0e3e5] text-xs font-bold text-[#424752]">
                <th class="p-3">ID Очага</th>
                <th class="p-3">Локация / Регион</th>
                <th class="p-3">Категория (Root Cause)</th>
                <th class="p-3">Уровень угрозы</th>
                <th class="p-3">Заболевших</th>
                <th class="p-3">Статус</th>
              </tr>
            </thead>
            <tbody class="text-xs divide-y divide-[#e0e3e5]">
              {#each fociResults as foci}
                <tr class="hover:bg-[#f7f9fb] transition-colors">
                  <td class="p-3 font-mono font-bold text-[#003f87]">{foci.id}</td>
                  <td class="p-3 font-medium">{foci.location}</td>
                  <td class="p-3 font-semibold">{foci.category}</td>
                  <td class="p-3">
                    <span class="px-2 py-1 rounded-full text-[10px] font-bold {foci.hazardLevel === 'HIGH' ? 'bg-[#ffdad6] text-[#93000a]' : foci.hazardLevel === 'MEDIUM' ? 'bg-amber-100 text-amber-900' : 'bg-emerald-100 text-emerald-900'}">
                      {foci.hazardLevel}
                    </span>
                  </td>
                  <td class="p-3 font-bold">{foci.cases} чел.</td>
                  <td class="p-3">
                    <span class="px-2 py-0.5 rounded text-[10px] font-medium bg-[#eceef0] text-[#424752]">
                      {foci.status}
                    </span>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      </section>
    {:else if activeTab === 'privacy'}
      <section id="panel-privacy" role="tabpanel" aria-labelledby="tab-privacy" class="space-y-6">
        <PrivacySettings {currentUser} />
      </section>
    {/if}
  </main>

  <footer class="max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-6 text-center text-xs text-[#727784] border-t border-[#e0e3e5]">
    <p>ФБУН «Российский научно-исследовательский институт эпидемиологии» • Единая консоль управления</p>
  </footer>
</div>
