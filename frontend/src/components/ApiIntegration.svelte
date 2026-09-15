<script>
  import { onMount, createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export let apiBaseUrl = '/api/v1';
  export let currentUser = {
    id: 101,
    username: 'admin_user',
    role: 'ADMIN',
    full_name: 'Иванов И.И. (Администратор)'
  };

  $: isAdmin = currentUser && currentUser.role === 'ADMIN';

  // Search filter states
  let searchQuery = '';
  let selectedAuthor = '';
  let selectedYear = '';

  // API Data view states
  let documents = [];
  let isLoading = false;
  let searchError = '';

  // Upload modal & form states
  let isUploadModalOpen = false;
  let uploadTitle = '';
  let uploadAuthor = '';
  let uploadYear = new Date().getFullYear().toString();
  let uploadDocType = 'Протокол расследования';
  let uploadDescription = '';
  let uploadFileName = '';
  let simulateNetworkError = false;

  let isUploading = false;
  let uploadError = '';
  let uploadSuccess = '';
  let feedbackNotice = null;

  async function fetchDocuments() {
    isLoading = true;
    searchError = '';

    try {
      const params = new URLSearchParams();
      if (searchQuery.trim()) params.append('query', searchQuery.trim());
      if (selectedAuthor.trim()) params.append('author', selectedAuthor.trim());
      if (selectedYear.toString().trim()) params.append('year', selectedYear.toString().trim());

      const response = await fetch(`${apiBaseUrl}/documents/search?${params.toString()}`);
      if (!response.ok) {
        throw new Error(`Ошибка сервера (${response.status}): Не удалось загрузить данные.`);
      }

      const data = await response.json();
      if (Array.isArray(data)) {
        documents = data;
      } else if (data && Array.isArray(data.results)) {
        documents = data.results;
      } else {
        documents = [];
      }
    } catch (err) {
      documents = [];
      searchError = err.message || 'Ошибка подключения к серверу API.';
    } finally {
      isLoading = false;
    }
  }

  function handleSearchSubmit(event) {
    if (event) event.preventDefault();
    fetchDocuments();
  }

  function handleResetSearch() {
    searchQuery = '';
    selectedAuthor = '';
    selectedYear = '';
    fetchDocuments();
  }

  function openUploadModal() {
    uploadError = '';
    uploadSuccess = '';
    isUploadModalOpen = true;
  }

  function closeUploadModal() {
    isUploadModalOpen = false;
  }

  function handleFileSelect(event) {
    const files = event.target.files;
    if (files && files.length > 0) {
      uploadFileName = files[0].name;
    }
  }

  async function handleUploadSubmit(event) {
    if (event) event.preventDefault();
    uploadError = '';
    uploadSuccess = '';

    if (!uploadTitle.trim() || !uploadAuthor.trim() || !uploadYear) {
      uploadError = 'Заполните все обязательные поля формы.';
      return;
    }

    isUploading = true;

    try {
      if (simulateNetworkError) {
        await new Promise(resolve => setTimeout(resolve, 300));
        throw new Error('Ошибка сети при загрузке документа. Попробуйте еще раз.');
      }

      const response = await fetch(`${apiBaseUrl}/documents/upload`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          title: uploadTitle.trim(),
          author: uploadAuthor.trim(),
          year: parseInt(uploadYear, 10),
          docType: uploadDocType,
          description: uploadDescription.trim(),
          fileName: uploadFileName || 'document.pdf'
        })
      });

      if (!response.ok) {
        let errData = {};
        try {
          errData = await response.json();
        } catch (e) {}
        throw new Error(errData.message || 'Ошибка сервера при загрузке документа.');
      }

      const newDoc = await response.json();
      documents = [newDoc, ...documents];
      uploadSuccess = 'Документ успешно загружен в каталог.';

      // Reset form fields on success only
      uploadTitle = '';
      uploadAuthor = '';
      uploadYear = new Date().getFullYear().toString();
      uploadDocType = 'Протокол расследования';
      uploadDescription = '';
      uploadFileName = '';
      isUploadModalOpen = false;
      feedbackNotice = { type: 'success', message: 'Документ успешно добавлен в базу.' };
    } catch (err) {
      // User input survives in form upon backend request failure
      uploadError = err.message || 'Ошибка взаимодействия с API при загрузке.';
    } finally {
      isUploading = false;
    }
  }

  async function handleDeleteDocument(id) {
    try {
      const response = await fetch(`${apiBaseUrl}/documents/${id}`, {
        method: 'DELETE'
      });
      if (!response.ok) {
        throw new Error('Не удалось удалить документ.');
      }
      documents = documents.filter(doc => doc.id !== id && doc.id !== String(id));
      feedbackNotice = { type: 'success', message: 'Документ успешно удален.' };
    } catch (err) {
      feedbackNotice = { type: 'error', message: err.message || 'Ошибка удаления документа' };
    }
  }

  onMount(() => {
    fetchDocuments();
  });
</script>

<div class="api-integration-wrapper bg-[#f7f9fb] p-6 rounded-xl border border-[#e0e3e5] shadow-sm font-sans text-[#191c1e]">
  <header class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 border-b border-[#e0e3e5] pb-4 mb-6">
    <div>
      <h2 class="text-xl font-bold text-[#003f87] flex items-center gap-2">
        <span>🔄 Интеграция API каталога</span>
      </h2>
      <p class="text-xs text-[#424752] mt-1">
        Взаимодействие с реальными эндпоинтами бэкенда для синхронизации состояния
      </p>
    </div>

    {#if isAdmin}
      <button
        type="button"
        id="api-open-upload-btn"
        on:click={openUploadModal}
        class="min-h-[44px] min-w-[44px] px-4 py-2 bg-[#003f87] hover:bg-[#002b5e] focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none text-white text-xs font-bold rounded-lg transition-colors shadow-sm flex items-center gap-1.5"
      >
        <span>+ Загрузить документ</span>
      </button>
    {/if}
  </header>

  {#if feedbackNotice}
    <div
      role={feedbackNotice.type === 'error' ? 'alert' : 'status'}
      aria-live="polite"
      class="mb-6 p-4 rounded-xl border flex items-center justify-between shadow-sm transition-all {feedbackNotice.type === 'error' ? 'bg-[#ffdad6] text-[#93000a] border-[#ba1a1a]/30' : 'bg-[#d9e3f1] text-[#001a40] border-[#003f87]/30'}"
    >
      <div class="flex items-center gap-2">
        <span class="text-lg">{feedbackNotice.type === 'error' ? '⚠️' : '✓'}</span>
        <span class="text-xs font-semibold">{feedbackNotice.message}</span>
      </div>
      <button
        type="button"
        on:click={() => feedbackNotice = null}
        class="text-xs underline font-semibold ml-4 hover:opacity-80 focus:outline-none focus:ring-2 focus:ring-[#003f87]"
      >
        Закрыть
      </button>
    </div>
  {/if}

  <!-- Search controls form -->
  <form on:submit={handleSearchSubmit} class="bg-white p-4 rounded-xl border border-[#e0e3e5] mb-6 grid grid-cols-1 md:grid-cols-12 gap-3 items-end shadow-sm">
    <div class="md:col-span-5">
      <label for="api-search-query" class="block text-xs font-semibold text-[#191c1e] mb-1">Ключевое слово</label>
      <input
        id="api-search-query"
        type="text"
        bind:value={searchQuery}
        placeholder="Поиск по документам..."
        class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-lg text-xs text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
      />
    </div>

    <div class="md:col-span-4">
      <label for="api-search-author" class="block text-xs font-semibold text-[#191c1e] mb-1">Автор / Организация</label>
      <input
        id="api-search-author"
        type="text"
        bind:value={selectedAuthor}
        placeholder="НИИ Эпидемиологии"
        class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-lg text-xs text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
      />
    </div>

    <div class="md:col-span-3 flex gap-2">
      <button
        type="submit"
        id="api-search-submit-btn"
        disabled={isLoading}
        class="flex-1 min-h-[44px] min-w-[44px] bg-[#003f87] hover:bg-[#002b5e] focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none text-white text-xs font-bold rounded-lg transition-colors flex items-center justify-center disabled:opacity-60"
      >
        {isLoading ? 'Загрузка...' : 'Найти'}
      </button>

      {#if searchQuery || selectedAuthor || selectedYear}
        <button
          type="button"
          id="api-search-reset-btn"
          on:click={handleResetSearch}
          class="min-h-[44px] min-w-[44px] px-3 border border-[#c2c6d4] text-[#424752] hover:bg-[#eceef0] focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none text-xs font-medium rounded-lg transition-colors"
        >
          Сброс
        </button>
      {/if}
    </div>
  </form>

  <!-- Document List / Loading / Error Section -->
  <section aria-label="Результаты запроса API">
    {#if isLoading}
      <div id="api-loading-state" role="status" class="bg-white border border-[#e0e3e5] rounded-xl p-8 text-center space-y-3 shadow-sm my-4">
        <div class="inline-block animate-spin rounded-full h-8 w-8 border-4 border-[#003f87] border-t-transparent"></div>
        <p class="text-xs text-[#424752] font-semibold">Отправка запроса к API сервера...</p>
      </div>
    {:else if searchError}
      <div id="api-error-state" role="alert" class="bg-[#ffdad6] border border-[#ba1a1a]/30 rounded-xl p-6 text-center space-y-3 shadow-sm my-4">
        <div class="text-lg font-bold text-[#93000a]">⚠️ {searchError}</div>
        <button
          type="button"
          on:click={fetchDocuments}
          class="min-h-[44px] min-w-[44px] px-4 py-2 bg-[#ba1a1a] hover:bg-[#93000a] text-white text-xs font-semibold rounded-lg focus:ring-2 focus:ring-[#ba1a1a]/50 focus:outline-none transition-colors"
        >
          Повторить запрос
        </button>
      </div>
    {:else if documents.length === 0}
      <div id="api-empty-state" role="status" class="bg-white border border-[#e0e3e5] rounded-xl p-8 text-center space-y-2 shadow-sm my-4">
        <h3 class="text-base font-bold text-[#191c1e]">нет материалов</h3>
        <p class="text-xs text-[#424752]">По текущему запросу к бэкенду ничего не найдено.</p>
      </div>
    {:else}
      <div id="api-document-grid" class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {#each documents as doc (doc.id || doc.title)}
          <article class="bg-white border border-[#e0e3e5] rounded-xl p-4 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
            <div>
              <div class="flex items-center justify-between mb-2">
                <span class="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#d9e3f1] text-[#003f87]">
                  {doc.docType || doc.doc_type || 'Документ'}
                </span>
                <span class="text-xs text-[#727784] font-medium">{doc.year || doc.publicationYear || '—'} г.</span>
              </div>
              <h3 class="text-sm font-bold text-[#191c1e] mb-1 line-clamp-2">{doc.title}</h3>
              <p class="text-xs text-[#424752] mb-3 line-clamp-2">{doc.description || 'Без описания'}</p>
            </div>
            {#if isAdmin}
              <div class="pt-2 border-t border-[#e0e3e5] flex justify-end">
                <button
                  type="button"
                  on:click={() => handleDeleteDocument(doc.id)}
                  class="min-h-[44px] min-w-[44px] px-3 py-1.5 border border-[#ba1a1a] text-[#ba1a1a] hover:bg-[#ffdad6] text-xs font-medium rounded-md focus:ring-2 focus:ring-[#ba1a1a]/50 focus:outline-none transition-colors"
                >
                  Удалить
                </button>
              </div>
            {/if}
          </article>
        {/each}
      </div>
    {/if}
  </section>

  <!-- Upload Modal -->
  {#if isUploadModalOpen}
    <div id="api-upload-modal" class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div class="bg-white rounded-xl border border-[#e0e3e5] max-w-lg w-full p-6 shadow-xl">
        <div class="flex items-center justify-between pb-3 border-b border-[#e0e3e5] mb-4">
          <h3 class="text-base font-bold text-[#191c1e]">Загрузка документа через API</h3>
          <button
            type="button"
            on:click={closeUploadModal}
            class="text-[#727784] hover:text-[#191c1e] text-lg font-bold p-1 rounded-full focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none"
          >
            ✕
          </button>
        </div>

        {#if uploadError}
          <div id="api-upload-error-alert" role="alert" class="mb-4 p-3 rounded-lg bg-[#ffdad6] text-[#93000a] text-xs font-medium border border-[#ba1a1a]/30">
            ⚠️ {uploadError}
          </div>
        {/if}

        <form on:submit={handleUploadSubmit} class="space-y-4" novalidate>
          <div>
            <label for="api-upload-title-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
              Название документа *
            </label>
            <input
              id="api-upload-title-input"
              type="text"
              bind:value={uploadTitle}
              placeholder="Введите название"
              required
              class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-xs text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
            />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label for="api-upload-author-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
                Автор / Организация *
              </label>
              <input
                id="api-upload-author-input"
                type="text"
                bind:value={uploadAuthor}
                placeholder="НИИ Эпидемиологии"
                required
                class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-xs text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
              />
            </div>

            <div>
              <label for="api-upload-year-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
                Год публикации *
              </label>
              <input
                id="api-upload-year-input"
                type="number"
                bind:value={uploadYear}
                min="1990"
                max="2030"
                required
                class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-xs text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
              />
            </div>
          </div>

          <div>
            <label for="api-upload-description-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
              Описание
            </label>
            <textarea
              id="api-upload-description-input"
              bind:value={uploadDescription}
              rows="3"
              placeholder="Краткое описание"
              class="w-full p-2.5 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-xs text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/50"
            ></textarea>
          </div>

          <div class="pt-2 border-t border-[#f2f4f6]">
            <label class="flex items-center gap-2 text-xs text-[#424752] cursor-pointer">
              <input
                id="api-simulate-error-checkbox"
                type="checkbox"
                bind:checked={simulateNetworkError}
                class="rounded border-[#c2c6d4] text-[#003f87] focus:ring-2 focus:ring-[#003f87]/50"
              />
              <span>Симулировать сбой сети при отправке</span>
            </label>
          </div>

          <div class="flex items-center justify-end gap-3 pt-4 border-t border-[#e0e3e5]">
            <button
              type="button"
              on:click={closeUploadModal}
              class="min-h-[44px] min-w-[44px] px-4 py-2 border border-[#c2c6d4] text-[#424752] hover:bg-[#eceef0] focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none text-xs font-medium rounded-md"
            >
              Отмена
            </button>
            <button
              type="submit"
              id="api-upload-submit-btn"
              disabled={isUploading}
              class="min-h-[44px] min-w-[44px] px-4 py-2 bg-[#003f87] hover:bg-[#002b5e] focus:ring-2 focus:ring-[#003f87]/50 focus:outline-none text-white text-xs font-bold rounded-md disabled:opacity-60 shadow-sm"
            >
              {isUploading ? 'Загрузка...' : 'Сохранить'}
            </button>
          </div>
        </form>
      </div>
    </div>
  {/if}
</div>
