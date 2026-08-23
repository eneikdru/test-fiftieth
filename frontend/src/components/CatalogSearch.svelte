<script>
  import { createEventDispatcher } from 'svelte';

  const dispatch = createEventDispatcher();

  export function getApiBaseUrl() {
    return '/api/v1';
  }

  // Props / Initial state
  export let currentUser = {
    id: 101,
    username: 'исследователь',
    role: 'RESEARCHER', // 'RESEARCHER' | 'ADMIN'
    full_name: 'Сотрудник института'
  };

  $: isAdmin = currentUser && currentUser.role === 'ADMIN';

  // Search filter states
  let searchQuery = '';
  let selectedAuthor = '';
  let selectedYear = '';
  let isSearchSubmitted = false;

  // Document catalog sample data
  let documents = [
    {
      id: 'doc-1',
      title: 'Протокол расследования вспышки гриппа A(H3N2)',
      author: 'НИИ Эпидемиологии',
      year: 2023,
      docType: 'Протокол расследования',
      fileName: 'protocol_influenza_2023.pdf',
      fileSize: '2.4 МБ',
      description: 'Материалы эпидемиологического расследования вспышки гриппа в Северо-Западном регионе.'
    },
    {
      id: 'doc-2',
      title: 'Отчёт эпиднадзора за острыми респираторными инфекциями',
      author: 'Центр гигиены и эпидемиологии',
      year: 2022,
      docType: 'Отчёт эпиднадзора',
      fileName: 'surveillance_report_2022.pdf',
      fileSize: '4.1 МБ',
      description: 'Годовой статистический и аналитический отчет по заболеваемости ОРВИ.'
    },
    {
      id: 'doc-3',
      title: 'Методическое руководство по генотипированию штаммов',
      author: 'НИИ Эпидемиологии',
      year: 2021,
      docType: 'Методическое руководство',
      fileName: 'methodology_genotyping.docx',
      fileSize: '1.8 МБ',
      description: 'Практические рекомендации по лабораторому анализу и идентификации штаммов.'
    }
  ];

  // Upload modal / form states
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

  // Filtered documents reactive computation
  $: filteredDocuments = documents.filter(doc => {
    const matchesQuery = !searchQuery.trim() ||
      doc.title.toLowerCase().includes(searchQuery.trim().toLowerCase()) ||
      doc.description.toLowerCase().includes(searchQuery.trim().toLowerCase());

    const matchesAuthor = !selectedAuthor ||
      doc.author.toLowerCase().includes(selectedAuthor.toLowerCase());

    const matchesYear = !selectedYear ||
      doc.year.toString() === selectedYear.toString();

    return matchesQuery && matchesAuthor && matchesYear;
  });

  function handleSearchSubmit(event) {
    if (event) event.preventDefault();
    isSearchSubmitted = true;
  }

  function handleResetSearch() {
    searchQuery = '';
    selectedAuthor = '';
    selectedYear = '';
    isSearchSubmitted = false;
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
        // Simulate network failure
        await new Promise(resolve => setTimeout(resolve, 300));
        throw new Error('Ошибка сети при загрузке документа. Попробуйте еще раз.');
      }

      const response = await fetch(`${getApiBaseUrl()}/documents/upload`, {
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
        throw new Error(errData.message || 'Ошибка сети при загрузке документа. Попробуйте еще раз.');
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
    } catch (err) {
      // Key AC Requirement: Entered metadata remains intact in the form!
      uploadError = err.message || 'Ошибка сети при загрузке документа. Попробуйте еще раз.';
    } finally {
      isUploading = false;
    }
  }

  function handleDeleteDocument(id) {
    documents = documents.filter(doc => doc.id !== id);
  }

  function handleDownload(doc) {
    dispatch('download', { document: doc });
    alert(`Загрузка документа: ${doc.fileName}`);
  }
</script>

<div class="catalog-container max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-6 font-sans text-[#191c1e] bg-[#f7f9fb] min-h-screen">
  <!-- Top Navigation & Header -->
  <header class="mb-8 flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#c2c6d4] pb-5">
    <div>
      <div class="flex items-center gap-2 mb-1">
        <span class="text-xs font-semibold uppercase text-[#424752] tracking-wider">Каталог материалов</span>
      </div>
      <h1 class="text-2xl md:text-3xl font-bold text-[#003f87]">
        База знаний по эпидемиологии
      </h1>
      <p class="text-sm text-[#424752] mt-1">
        Поиск и управление эпидемиологическими протоколами, отчётами и руководствами
      </p>
    </div>

    <!-- Admin / User Status Bar -->
    <div class="flex items-center gap-3 bg-white p-3 rounded-lg border border-[#e0e3e5] shadow-sm self-start md:self-auto">
      <div class="w-9 h-9 rounded-full bg-[#003f87] text-white flex items-center justify-center font-bold text-sm">
        {(currentUser?.full_name || currentUser?.username || 'С')[0]}
      </div>
      <div>
        <div class="text-xs font-medium text-[#191c1e]">{currentUser?.full_name || currentUser?.username}</div>
        <div class="text-[11px] text-[#424752]">
          Роль: <span class="font-semibold text-[#003f87]">{isAdmin ? 'Администратор' : 'Сотрудник'}</span>
        </div>
      </div>

      {#if isAdmin}
        <button
          type="button"
          on:click={openUploadModal}
          class="ml-2 px-3 py-2 bg-[#003f87] hover:bg-[#002b5e] text-white rounded-md text-xs font-medium transition-colors flex items-center gap-1.5 shadow-sm"
        >
          <span>+ Загрузить документ</span>
        </button>
      {/if}
    </div>
  </header>

  <!-- Search & Filter Controls -->
  <section class="bg-white p-5 rounded-xl border border-[#e0e3e5] shadow-sm mb-8">
    <h2 class="text-lg font-bold text-[#191c1e] mb-4 flex items-center gap-2">
      <span>🔍 Поиск документов</span>
    </h2>

    <form on:submit={handleSearchSubmit} class="grid grid-cols-1 md:grid-cols-12 gap-4">
      <!-- Query Input -->
      <div class="md:col-span-5">
        <label for="search-query-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
          Название или ключевое слово
        </label>
        <input
          id="search-query-input"
          type="text"
          bind:value={searchQuery}
          placeholder="Например: грипп, протокол, генотипирование..."
          class="w-full h-11 px-3.5 bg-[#f7f9fb] border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/30 focus:border-[#003f87]"
        />
      </div>

      <!-- Author / Organization Filter -->
      <div class="md:col-span-3">
        <label for="search-author-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
          Автор / Организация
        </label>
        <input
          id="search-author-input"
          type="text"
          bind:value={selectedAuthor}
          placeholder="НИИ Эпидемиологии"
          class="w-full h-11 px-3.5 bg-[#f7f9fb] border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/30 focus:border-[#003f87]"
        />
      </div>

      <!-- Year Filter -->
      <div class="md:col-span-2">
        <label for="search-year-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
          Год публикации
        </label>
        <select
          id="search-year-input"
          bind:value={selectedYear}
          class="w-full h-11 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-lg text-sm text-[#191c1e] focus:outline-none focus:ring-2 focus:ring-[#003f87]/30 focus:border-[#003f87]"
        >
          <option value="">Все года</option>
          <option value="2024">2024</option>
          <option value="2023">2023</option>
          <option value="2022">2022</option>
          <option value="2021">2021</option>
          <option value="2020">2020</option>
        </select>
      </div>

      <!-- Action Buttons -->
      <div class="md:col-span-2 flex items-end gap-2">
        <button
          type="submit"
          id="search-submit-btn"
          class="flex-1 h-11 bg-[#003f87] hover:bg-[#002b5e] text-white font-medium text-sm rounded-lg transition-colors flex items-center justify-center shadow-sm"
        >
          Найти
        </button>
        {#if searchQuery || selectedAuthor || selectedYear}
          <button
            type="button"
            id="search-reset-btn"
            on:click={handleResetSearch}
            class="h-11 px-3 border border-[#c2c6d4] text-[#424752] hover:bg-[#eceef0] text-xs font-medium rounded-lg transition-colors"
            title="Сбросить фильтры"
          >
            Сброс
          </button>
        {/if}
      </div>
    </form>
  </section>

  <!-- Document List Section -->
  <main>
    <div class="flex items-center justify-between mb-4">
      <h2 class="text-xl font-bold text-[#191c1e]">
        Результаты поиска <span class="text-sm font-normal text-[#424752]">({filteredDocuments.length})</span>
      </h2>
    </div>

    <!-- Empty State (When search query yields no matches) -->
    {#if filteredDocuments.length === 0}
      <div
        id="empty-catalog-message"
        role="status"
        class="bg-white border border-[#e0e3e5] rounded-xl p-8 text-center space-y-3 shadow-sm my-6"
      >
        <div class="w-12 h-12 bg-[#eceef0] rounded-full flex items-center justify-center mx-auto text-[#727784] text-xl font-bold">
          📄
        </div>
        <h3 class="text-lg font-bold text-[#191c1e]">нет материалов</h3>
        <p class="text-sm text-[#424752] max-w-md mx-auto">
          По вашему запросу не найдено ни одного документа. Проверьте правильность фильтров или сбросьте параметры поиска.
        </p>
        <button
          type="button"
          on:click={handleResetSearch}
          class="px-4 py-2 border border-[#003f87] text-[#003f87] hover:bg-[#003f87]/5 rounded-lg text-xs font-semibold transition-colors mt-2"
        >
          Показать все материалы
        </button>
      </div>
    {:else}
      <!-- Document Grid / List -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
        {#each filteredDocuments as doc (doc.id)}
          <article class="bg-white border border-[#e0e3e5] rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
            <div>
              <div class="flex items-start justify-between gap-2 mb-2">
                <span class="inline-block px-2.5 py-0.5 rounded-full text-[11px] font-medium bg-[#d9e3f1] text-[#131c26]">
                  {doc.docType}
                </span>
                <span class="text-xs text-[#727784] font-medium">{doc.year} г.</span>
              </div>

              <h3 class="text-base font-bold text-[#191c1e] mb-2 line-clamp-2 leading-snug">
                {doc.title}
              </h3>

              <p class="text-xs text-[#424752] mb-3 line-clamp-3 leading-relaxed">
                {doc.description}
              </p>

              <div class="text-xs text-[#727784] mb-4 space-y-1 border-t border-[#f2f4f6] pt-3">
                <div class="flex items-center justify-between">
                  <span>Организация:</span>
                  <span class="font-medium text-[#191c1e]">{doc.author}</span>
                </div>
                <div class="flex items-center justify-between">
                  <span>Файл:</span>
                  <span class="font-medium text-[#191c1e]">{doc.fileName} ({doc.fileSize})</span>
                </div>
              </div>
            </div>

            <div class="flex items-center justify-between gap-2 pt-3 border-t border-[#e0e3e5]">
              <button
                type="button"
                on:click={() => handleDownload(doc)}
                class="flex-1 py-2 px-3 bg-[#003f87] hover:bg-[#002b5e] text-white text-xs font-medium rounded-md transition-colors flex items-center justify-center gap-1 shadow-sm"
              >
                <span>Скачать</span>
              </button>

              {#if isAdmin}
                <button
                  type="button"
                  on:click={() => handleDeleteDocument(doc.id)}
                  class="py-2 px-3 border border-[#ba1a1a] text-[#ba1a1a] hover:bg-[#ffdad6] text-xs font-medium rounded-md transition-colors"
                  title="Удалить документ"
                >
                  Удалить
                </button>
              {/if}
            </div>
          </article>
        {/each}
      </div>
    {/if}
  </main>

  <!-- Admin Document Upload Modal -->
  {#if isUploadModalOpen}
    <div class="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
      <div class="bg-white rounded-xl border border-[#e0e3e5] max-w-lg w-full p-6 shadow-xl my-8">
        <div class="flex items-center justify-between pb-3 border-b border-[#e0e3e5] mb-4">
          <h3 class="text-lg font-bold text-[#191c1e]">Загрузка документа в каталог</h3>
          <button
            type="button"
            on:click={closeUploadModal}
            class="text-[#727784] hover:text-[#191c1e] text-xl font-bold p-1 rounded-full"
          >
            ✕
          </button>
        </div>

        {#if uploadError}
          <div role="alert" class="mb-4 p-3.5 rounded-lg bg-[#ffdad6] text-[#93000a] text-xs font-medium border border-[#ba1a1a]/30">
            ⚠ {uploadError}
          </div>
        {/if}

        {#if uploadSuccess}
          <div role="status" class="mb-4 p-3.5 rounded-lg bg-[#d9e3f1] text-[#001a40] text-xs font-medium border border-[#003f87]/30">
            ✓ {uploadSuccess}
          </div>
        {/if}

        <form on:submit={handleUploadSubmit} class="space-y-4" novalidate>
          <div>
            <label for="upload-title-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
              Название документа *
            </label>
            <input
              id="upload-title-input"
              type="text"
              bind:value={uploadTitle}
              placeholder="Введите полное название"
              required
              class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-sm text-[#191c1e] focus:outline-none focus:border-[#003f87]"
            />
          </div>

          <div class="grid grid-cols-2 gap-3">
            <div>
              <label for="upload-author-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
                Автор / Организация *
              </label>
              <input
                id="upload-author-input"
                type="text"
                bind:value={uploadAuthor}
                placeholder="НИИ Эпидемиологии"
                required
                class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-sm text-[#191c1e] focus:outline-none focus:border-[#003f87]"
              />
            </div>

            <div>
              <label for="upload-year-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
                Год публикации *
              </label>
              <input
                id="upload-year-input"
                type="number"
                bind:value={uploadYear}
                min="1990"
                max="2030"
                required
                class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-sm text-[#191c1e] focus:outline-none focus:border-[#003f87]"
              />
            </div>
          </div>

          <div>
            <label for="upload-doctype-select" class="block text-xs font-semibold text-[#191c1e] mb-1">
              Тип документа
            </label>
            <select
              id="upload-doctype-select"
              bind:value={uploadDocType}
              class="w-full h-10 px-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-sm text-[#191c1e] focus:outline-none focus:border-[#003f87]"
            >
              <option value="Протокол расследования">Протокол расследования</option>
              <option value="Отчёт эпиднадзора">Отчёт эпиднадзора</option>
              <option value="Набор данных">Набор данных</option>
              <option value="Методическое руководство">Методическое руководство</option>
            </select>
          </div>

          <div>
            <label for="upload-description-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
              Описание
            </label>
            <textarea
              id="upload-description-input"
              bind:value={uploadDescription}
              rows="3"
              placeholder="Краткая аннотация или описание документа"
              class="w-full p-3 bg-[#f7f9fb] border border-[#c2c6d4] rounded-md text-sm text-[#191c1e] focus:outline-none focus:border-[#003f87]"
            ></textarea>
          </div>

          <div>
            <label for="upload-file-input" class="block text-xs font-semibold text-[#191c1e] mb-1">
              Файл (PDF, DOCX)
            </label>
            <input
              id="upload-file-input"
              type="file"
              on:change={handleFileSelect}
              accept=".pdf,.doc,.docx,.xlsx"
              class="w-full text-xs text-[#424752] file:mr-3 file:py-2 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-[#d9e3f1] file:text-[#003f87] hover:file:bg-[#003f87]/10"
            />
          </div>

          <!-- Network Failure Testing Switch -->
          <div class="pt-2 border-t border-[#f2f4f6]">
            <label class="flex items-center gap-2 text-xs text-[#424752] cursor-pointer">
              <input
                id="simulate-network-error-checkbox"
                type="checkbox"
                bind:checked={simulateNetworkError}
                class="rounded border-[#c2c6d4] text-[#003f87] focus:ring-[#003f87]"
              />
              <span>Симулировать сбой сети при отправке</span>
            </label>
          </div>

          <div class="flex items-center justify-end gap-3 pt-4 border-t border-[#e0e3e5]">
            <button
              type="button"
              on:click={closeUploadModal}
              class="px-4 py-2 border border-[#c2c6d4] text-[#424752] hover:bg-[#eceef0] text-xs font-medium rounded-md transition-colors"
            >
              Отмена
            </button>
            <button
              type="submit"
              id="upload-submit-btn"
              disabled={isUploading}
              class="px-4 py-2 bg-[#003f87] hover:bg-[#002b5e] text-white text-xs font-medium rounded-md transition-colors disabled:opacity-60 shadow-sm"
            >
              {isUploading ? 'Загрузка...' : 'Сохранить и загрузить'}
            </button>
          </div>
        </form>
      </div>
    </div>
  {/if}
</div>
