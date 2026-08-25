<script>
  import { createEventDispatcher } from 'svelte';
  import ImprintModal from './ImprintModal.svelte';

  const dispatch = createEventDispatcher();

  export function getApiBaseUrl() {
    return '/api/v1';
  }

  // Current user prop
  export let currentUser = {
    id: 'usr_101',
    username: 'исследователь',
    role: 'RESEARCHER',
    full_name: 'Сотрудник института',
    email: 'ivanov@epidemiology-inst.ru'
  };

  // Export states
  let requestedFormat = 'ZIP';
  let exportNotes = '';
  let isExporting = false;
  let exportError = '';
  let exportSuccess = '';
  let exportJob = null;

  // Erasure / Delete Account states
  let isDeleteModalOpen = false;
  let erasureReason = 'Отозвано согласие на обработку персональных данных (152-ФЗ)';
  let erasureScope = 'ALL_PERSONAL_DATA';
  let confirmationInput = '';
  let isDeleting = false;
  let deleteError = '';
  let deleteSuccess = '';
  let erasureJob = null;
  let showImprint = false;

  // Confirmation token expected for destructive action
  $: expectedConfirmationToken = `УДАЛИТЬ ${currentUser?.username || 'АККАУНТ'}`;
  $: isConfirmationValid = confirmationInput.trim() === expectedConfirmationToken;

  async function handleExportData(event) {
    if (event) event.preventDefault();
    exportError = '';
    exportSuccess = '';
    isExporting = true;

    try {
      const response = await fetch(`${getApiBaseUrl()}/privacy/export-requests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subject_id: currentUser?.username || currentUser?.id || 'usr_101',
          requested_format: requestedFormat,
          notes: exportNotes.trim()
        })
      });

      if (!response.ok) {
        let errData = {};
        try {
          errData = await response.json();
        } catch (e) {}
        throw new Error(errData.message || 'Ошибка при запросе экспорта данных. Попробуйте позже.');
      }

      exportJob = await response.json();
      exportSuccess = 'Запрос на экспорт данных успешно создан и обрабатывается.';
      dispatch('exportRequested', { job: exportJob });
    } catch (err) {
      exportError = err.message || 'Произошла ошибка при формировании запроса экспорта.';
    } finally {
      isExporting = false;
    }
  }

  function openDeleteModal() {
    deleteError = '';
    deleteSuccess = '';
    confirmationInput = '';
    isDeleteModalOpen = true;
  }

  function closeDeleteModal() {
    isDeleteModalOpen = false;
    confirmationInput = '';
    deleteError = '';
  }

  async function handleConfirmDelete(event) {
    if (event) event.preventDefault();
    deleteError = '';
    deleteSuccess = '';

    if (!isConfirmationValid) {
      deleteError = 'Код подтверждения введен неверно.';
      return;
    }

    isDeleting = true;

    try {
      const subjectId = currentUser?.username || currentUser?.id || 'usr_101';
      const response = await fetch(`${getApiBaseUrl()}/privacy/erasure-requests`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subject_id: subjectId,
          confirmation_token: `CONFIRM_ERASURE_${subjectId}`,
          reason: erasureReason,
          erasure_scope: erasureScope
        })
      });

      if (!response.ok) {
        let errData = {};
        try {
          errData = await response.json();
        } catch (e) {}
        throw new Error(errData.message || 'Ошибка при обработке запроса на удаление данных.');
      }

      erasureJob = await response.json();
      deleteSuccess = 'Запрос на удаление данных принят. Аккаунт и персональные данные будут удалены.';
      isDeleteModalOpen = false;
      dispatch('erasureRequested', { job: erasureJob });
    } catch (err) {
      deleteError = err.message || 'Не удалось выполнить запрос на удаление аккаунта.';
    } finally {
      isDeleting = false;
    }
  }
</script>

<div class="privacy-container max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-6 font-sans text-[#191c1e] bg-[#f7f9fb] min-h-screen">
  <!-- Top Navigation & Header -->
  <header class="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#c2c6d4] pb-5">
    <div>
      <div class="flex items-center gap-2 mb-1">
        <span class="text-xs font-semibold uppercase text-[#424752] tracking-wider">Управление правами 152-ФЗ</span>
      </div>
      <h1 class="text-2xl md:text-3xl font-bold text-[#003f87]">
        Конфиденциальность и персональные данные
      </h1>
      <p class="text-sm text-[#424752] mt-1">
        Экспорт выгрузки персональных данных и управление правами на удаление информации
      </p>
    </div>

    <div class="flex items-center gap-3 bg-white p-3 rounded-lg border border-[#e0e3e5] shadow-sm self-start md:self-auto">
      <div class="w-9 h-9 rounded-full bg-[#003f87] text-white flex items-center justify-center font-bold text-sm">
        {(currentUser?.full_name || currentUser?.username || 'С')[0]}
      </div>
      <div>
        <div class="text-xs font-medium text-[#191c1e]">{currentUser?.full_name || currentUser?.username}</div>
        <div class="text-[11px] text-[#424752]">{currentUser?.email || 'Пользователь системы'}</div>
      </div>
    </div>
  </header>

  {#if deleteSuccess}
    <div role="status" class="mb-6 p-4 rounded-xl bg-[#d9e3f1] text-[#001a40] text-sm border border-[#003f87]/30 shadow-sm flex items-start gap-3">
      <span class="text-xl">✓</span>
      <div>
        <h3 class="font-bold">Удаление подтверждено</h3>
        <p class="mt-0.5">{deleteSuccess}</p>
      </div>
    </div>
  {/if}

  <main class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <!-- EXPORT PERSONAL DATA CARD -->
    <section class="bg-white border border-[#e0e3e5] rounded-xl p-6 shadow-sm flex flex-col justify-between">
      <div>
        <div class="flex items-center gap-3 mb-4">
          <div class="p-2.5 bg-[#d3e4fe] text-[#003f87] rounded-lg">
            <span class="text-xl font-bold">📥</span>
          </div>
          <div>
            <h2 class="text-lg font-bold text-[#191c1e]">Экспорт персональных данных</h2>
            <p class="text-xs text-[#424752]">Запросить полную копию ваших данных в соответствии с 152-ФЗ</p>
          </div>
        </div>

        <p class="text-sm text-[#424752] mb-5 leading-relaxed">
          Вы имеете право получить копию всех обработанных персональных данных, включая историю активности, профиль и загруженные материалы в структурированном виде.
        </p>

        {#if exportError}
          <div role="alert" class="mb-4 p-3.5 rounded-lg bg-[#ffdad6] text-[#93000a] text-xs font-medium border border-[#ba1a1a]/30">
            ⚠ {exportError}
          </div>
        {/if}

        {#if exportSuccess}
          <div role="status" class="mb-4 p-3.5 rounded-lg bg-[#d9e3f1] text-[#001a40] text-xs font-medium border border-[#003f87]/30">
            ✓ {exportSuccess}
            {#if exportJob?.download_url}
              <div class="mt-2">
                <a href={exportJob.download_url} class="underline font-bold text-[#003f87]" target="_blank" rel="noreferrer">
                  Скачать архив данных
                </a>
              </div>
            {/if}
          </div>
        {/if}

        <form on:submit={handleExportData} class="space-y-4">
          <div>
            <label for="export-format-select" class="block text-xs font-semibold text-[#191c1e] mb-1">
              Формат архива экспорта
            </label>
            <select
              id="export-format-select"
              bind:value={requestedFormat}
              class="w-full h-11 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:border-[#003f87]"
            >
              <option value="ZIP">ZIP-архив (Документы и JSON)</option>
              <option value="JSON">JSON (Только структуры данных)</option>
            </select>
          </div>

          <div>
            <label for="export-notes-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
              Примечание к запросу (необязательно)
            </label>
            <input
              id="export-notes-input"
              type="text"
              bind:value={exportNotes}
              placeholder="Например: Запрос для личного архива"
              class="w-full h-11 px-3.5 bg-[#f7f9fb] border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:border-[#003f87]"
            />
          </div>

          <div class="pt-2">
            <button
              type="submit"
              id="export-data-btn"
              disabled={isExporting}
              class="w-full h-11 bg-[#003f87] hover:bg-[#002b5e] text-white font-medium text-sm rounded-lg transition-colors flex items-center justify-center gap-2 shadow-sm disabled:opacity-60"
            >
              <span>{isExporting ? 'Формирование архива...' : 'Запросить выгрузку данных'}</span>
            </button>
          </div>
        </form>
      </div>
    </section>

    <!-- DELETE ACCOUNT / ERASURE CARD -->
    <section class="bg-white border border-[#ffdad6] rounded-xl p-6 shadow-sm flex flex-col justify-between">
      <div>
        <div class="flex items-center gap-3 mb-4">
          <div class="p-2.5 bg-[#ffdad6] text-[#ba1a1a] rounded-lg">
            <span class="text-xl font-bold">🗑</span>
          </div>
          <div>
            <h2 class="text-lg font-bold text-[#ba1a1a]">Удаление аккаунта и данных</h2>
            <p class="text-xs text-[#424752]">Отозвать согласие и полностью уничтожить персональные данные</p>
          </div>
        </div>

        <p class="text-sm text-[#424752] mb-5 leading-relaxed">
          В соответствии с Федеральным законом № 152-ФЗ «О персональных данных» вы имеете право потребовать полного удаления или обезличивания ваших данных.
        </p>

        <div class="p-4 bg-[#ffdad6]/40 rounded-lg border border-[#ba1a1a]/20 mb-6">
          <h3 class="text-xs font-bold text-[#93000a] uppercase tracking-wide mb-1">⚠️ Внимание</h3>
          <p class="text-xs text-[#93000a]">
            Удаление профиля является необратимым действием. Все ваши настройки, доступ к материалам и история будут безвозвратно уничтожены.
          </p>
        </div>

        <div>
          <button
            type="button"
            id="open-delete-account-btn"
            on:click={openDeleteModal}
            class="w-full h-11 bg-[#ba1a1a] hover:bg-[#93000a] text-white font-medium text-sm rounded-lg transition-colors flex items-center justify-center gap-2 shadow-sm"
          >
            <span>Удалить аккаунт и данные</span>
          </button>
        </div>
      </div>
    </section>
  </main>

  <!-- SEVERE WARNING & CONFIRMATION MODAL -->
  {#if isDeleteModalOpen}
    <div id="delete-confirmation-modal" class="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div class="bg-white rounded-xl border-2 border-[#ba1a1a] max-w-lg w-full p-6 shadow-2xl my-8">
        <!-- Header -->
        <div class="flex items-start justify-between pb-3 border-b border-[#ffdad6] mb-4">
          <div class="flex items-center gap-3">
            <div class="w-10 h-10 rounded-full bg-[#ffdad6] text-[#ba1a1a] flex items-center justify-center font-bold text-xl">
              🚨
            </div>
            <div>
              <h3 class="text-lg font-bold text-[#ba1a1a]">Подтверждение удаления аккаунта</h3>
              <p class="text-xs text-[#737688]">Критическое действие • 152-ФЗ</p>
            </div>
          </div>
          <button
            type="button"
            on:click={closeDeleteModal}
            class="text-[#737688] hover:text-[#191c1e] text-xl font-bold p-1 rounded-full"
          >
            ✕
          </button>
        </div>

        {#if deleteError}
          <div id="delete-error-alert" role="alert" class="mb-4 p-3.5 rounded-lg bg-[#ffdad6] text-[#93000a] text-xs font-medium border border-[#ba1a1a]/30">
            ⚠ {deleteError}
          </div>
        {/if}

        <!-- Severe Warning Body -->
        <div class="space-y-4 mb-6">
          <div class="p-4 bg-[#ffdad6] rounded-lg border border-[#ba1a1a]/40 text-[#93000a] space-y-2">
            <h4 class="font-bold text-sm uppercase tracking-wide">⚠️ ВНИМАНИЕ: НЕОБРАТИМОЕ ДЕЙСТВИЕ</h4>
            <p class="text-xs leading-relaxed">
              Вы подготавливаете полное отзыв согласия на обработку персональных данных и стирание профиля. После подтверждения:
            </p>
            <ul class="list-disc list-inside text-xs space-y-1 font-medium pl-1">
              <li>Ваша учетная запись <strong class="underline">{currentUser?.username}</strong> будет немедленно заблокирована.</li>
              <li>Все персональные записи будут безвозвратно удалены из системы.</li>
              <li>Восстановление доступа будет невозможно.</li>
            </ul>
          </div>

          <form on:submit={handleConfirmDelete} class="space-y-4" novalidate>
            <div>
              <label for="erasure-reason-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
                Основание для удаления (152-ФЗ)
              </label>
              <input
                id="erasure-reason-input"
                type="text"
                bind:value={erasureReason}
                required
                class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-xs text-[#191c1e] focus:outline-none focus:border-[#ba1a1a]"
              />
            </div>

            <div>
              <label for="confirmation-input" class="block text-xs font-bold text-[#ba1a1a] mb-1">
                Для подтверждения введите фразовый код: <span class="bg-[#ffdad6] px-1.5 py-0.5 rounded select-all font-mono">{expectedConfirmationToken}</span>
              </label>
              <input
                id="confirmation-input"
                type="text"
                bind:value={confirmationInput}
                placeholder={expectedConfirmationToken}
                required
                class="w-full h-11 px-3.5 bg-[#f7f9fb] border-2 border-[#ba1a1a]/50 rounded-lg text-sm text-[#191c1e] focus:outline-none focus:border-[#ba1a1a] font-mono"
              />
            </div>

            <div class="flex items-center justify-end gap-3 pt-4 border-t border-[#e0e3e5]">
              <button
                type="button"
                on:click={closeDeleteModal}
                class="px-4 py-2.5 border border-[#c2c6d4] text-[#424752] hover:bg-[#eceef0] text-xs font-medium rounded-lg transition-colors"
              >
                Отмена
              </button>
              <button
                type="submit"
                id="confirm-delete-btn"
                disabled={!isConfirmationValid || isDeleting}
                class="px-5 py-2.5 bg-[#ba1a1a] hover:bg-[#93000a] text-white text-xs font-bold rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed shadow-md flex items-center gap-2"
              >
                <span>{isDeleting ? 'Удаление...' : 'Да, удалить аккаунт навсегда'}</span>
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  {/if}

  {#if showImprint}
    <ImprintModal on:close={() => showImprint = false} />
  {/if}

  <footer class="w-full py-6 mt-12 text-center text-xs text-[#727784] border-t border-[#e0e3e5] flex flex-col sm:flex-row items-center justify-between gap-2">
    <p>Российский научно-исследовательский институт эпидемиологии</p>
    <div>
      <button
        type="button"
        id="imprint-link-privacy"
        on:click={() => showImprint = true}
        class="text-[#003f87] hover:underline focus:outline-none focus:ring-2 focus:ring-[#003f87]/50 rounded px-1 font-semibold"
      >
        Выходные данные (Imprint / Impressum)
      </button>
    </div>
  </footer>
</div>
