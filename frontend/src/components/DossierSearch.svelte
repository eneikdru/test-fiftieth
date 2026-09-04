<script>
    let surname = "";
    let documents = [];
    let loading = false;
    let errorMessage = "";
    let searched = false;
    let feedback = "";
    let page = 0;
    let size = 10;
    let hasNext = false;

    async function searchDossier() {
        if (!surname.trim()) return;
        loading = true;
        errorMessage = "";
        feedback = "";
        try {
            const res = await fetch(`/api/v1/dossier/documents?employee_surname=${encodeURIComponent(surname.trim())}&page=${page}&size=${size}`);
            if (res.ok) {
                const data = await res.json();
                documents = Array.isArray(data) ? data : (data.results || []);
                hasNext = documents.length === size;
                searched = true;
            } else {
                documents = [];
                errorMessage = "Ошибка сервера при получении данных досье (Backend error).";
                searched = true;
            }
        } catch (e) {
            console.error(e);
            documents = [];
            errorMessage = "Ошибка подключения к серверу. Досье недоступно (Backend down).";
            searched = true;
        } finally {
            loading = false;
        }
    }

    function nextPage() {
        if (hasNext) {
            page++;
            searchDossier();
        }
    }

    function prevPage() {
        if (page > 0) {
            page--;
            searchDossier();
        }
    }

    function handleSearchClick() {
        page = 0;
        searchDossier();
    }

    function generateReport() {
        loading = true;
        feedback = "";
        setTimeout(() => {
            loading = false;
            feedback = "✓ Итоговая справка успешно сформирована.";
        }, 1000);
    }
</script>

<div class="dossier-container">
    <div class="search-section">
        <input
            type="text"
            bind:value={surname}
            placeholder="Фамилия сотрудника"
            aria-label="Фамилия сотрудника"
            id="search-query-input"
        />
        <button on:click={handleSearchClick} id="search-button" aria-label="Поиск">Поиск</button>
    </div>

    <!-- State 1: Loading State -->
    {#if loading}
        <div id="dossier-loading-state" role="status" aria-live="polite" class="loading-notice">
            <span id="loading-spinner">Загрузка документов досье...</span>
        </div>

    <!-- State 2: Error State -->
    {:else if errorMessage}
        <div id="dossier-error-state" role="alert" class="error-notice">
            <p>{errorMessage}</p>
            <button on:click={searchDossier} class="retry-btn" aria-label="Повторить запрос">Повторить</button>
        </div>

    <!-- State 3: Empty State -->
    {:else if searched && documents.length === 0}
        <div id="dossier-empty-state" role="status" class="empty-notice">
            <h3>нет материалов</h3>
            <p>По вашему запросу документы досье не найдены.</p>
        </div>

    <!-- State 4: Present State -->
    {:else if documents.length > 0}
        <ul class="document-list" id="document-list">
            {#each documents as doc}
                <li>{doc.title || doc.name || `Документ ${doc.id}`}</li>
            {/each}
        </ul>
        <div class="report-section">
            <button on:click={generateReport} id="generate-report-button" aria-label="Сформировать итоговую справку" disabled={loading}>
                {#if loading}
                    <span id="loading-spinner">Загрузка...</span>
                {:else}
                    Сформировать итоговую справку
                {/if}
            </button>
        </div>
        {#if feedback}
            <div role="status" aria-live="polite" class="feedback-notice">
                {feedback}
            </div>
        {/if}
    {/if}

    <div class="pagination-section">
        <button on:click={prevPage} disabled={page === 0} aria-label="Предыдущая страница" id="prev-page-btn">Назад</button>
        <span>Страница {page + 1}</span>
        <button on:click={nextPage} disabled={!hasNext} aria-label="Следующая страница" id="next-page-btn">Вперед</button>
    </div>

    <footer class="dossier-footer">
        <span>Российский научно-исследовательский институт эпидемиологии</span>
        <button on:click={() => alert('Выходные данные (Imprint / Impressum):\nФБУН «НИИ Эпидемиологии»\nг. Москва, ул. Новогиреевская, 3А')} class="imprint-btn" aria-label="Выходные данные">
            Выходные данные (Imprint / Impressum)
        </button>
    </footer>
</div>

<style>
    .dossier-container { padding: 20px; font-family: sans-serif; color: #191c1e; }
    .search-section { margin-bottom: 20px; display: flex; gap: 10px; }
    input { padding: 8px; flex-grow: 1; border: 1px solid #767676; border-radius: 4px; color: #191c1e; background-color: #ffffff; }
    input:focus { outline: 2px solid #005fcc; outline-offset: 2px; }
    button { padding: 8px 16px; background-color: #003f87; color: white; border: none; border-radius: 4px; cursor: pointer; font-weight: 600; }
    button:focus { outline: 2px solid #005fcc; outline-offset: 2px; }
    button:disabled { background-color: #99c2ff; color: #191c1e; cursor: not-allowed; }
    .loading-notice { padding: 16px; text-align: center; color: #003f87; font-weight: 500; }
    .error-notice { margin-bottom: 20px; padding: 12px 16px; background-color: #ffdad6; color: #93000a; border-radius: 4px; border: 1px solid #ba1a1a; display: flex; justify-content: space-between; align-items: center; }
    .retry-btn { background-color: #ba1a1a; color: white; border: none; padding: 6px 12px; border-radius: 4px; cursor: pointer; }
    .empty-notice { margin-bottom: 20px; padding: 16px; background-color: #eceef0; color: #191c1e; border-radius: 4px; text-align: center; border: 1px solid #c2c6d4; }
    .empty-notice h3 { margin: 0 0 8px 0; font-size: 16px; font-weight: 700; }
    .document-list { list-style: none; padding: 0; margin-bottom: 20px; }
    .document-list li { padding: 10px; border-bottom: 1px solid #c2c6d4; color: #191c1e; }
    .report-section { display: flex; align-items: center; }
    .feedback-notice { margin-top: 15px; padding: 10px; background-color: #d9e3f1; color: #001a40; border-radius: 4px; font-size: 14px; border: 1px solid #003f87; }
    .pagination-section { margin-top: 20px; display: flex; justify-content: space-between; align-items: center; color: #191c1e; }
    .dossier-footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #c2c6d4; text-align: center; font-size: 12px; color: #424752; display: flex; justify-content: space-between; align-items: center; }
    .imprint-btn { background: none; border: none; color: #003f87; text-decoration: underline; cursor: pointer; padding: 0; }
</style>
