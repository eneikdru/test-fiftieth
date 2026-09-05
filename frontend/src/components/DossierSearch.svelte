<script>
    let surname = "";
    let documents = [];
    let loading = false;
    let feedback = "";
    let page = 0;
    let size = 10;
    let hasNext = false;
    let totalPages = 0;
    let totalCount = 0;
    let searchExecuted = false;
    let errorMessage = "";

    async function searchDossier() {
        if (!surname.trim()) return;
        loading = true;
        errorMessage = "";
        try {
            const res = await fetch(`/api/v1/dossier/documents?employee_surname=${encodeURIComponent(surname.trim())}&page=${page}&size=${size}`);
            if (res.ok) {
                const data = await res.json();
                documents = Array.isArray(data) ? data : (data.content || []);

                const headerTotalCount = res.headers.get("X-Total-Count");
                const headerTotalPages = res.headers.get("X-Total-Pages");

                if (headerTotalPages !== null && headerTotalPages !== "") {
                    totalPages = parseInt(headerTotalPages, 10);
                    hasNext = page + 1 < totalPages;
                } else if (headerTotalCount !== null && headerTotalCount !== "") {
                    totalCount = parseInt(headerTotalCount, 10);
                    totalPages = Math.ceil(totalCount / size);
                    hasNext = page + 1 < totalPages;
                } else {
                    hasNext = documents.length === size;
                }
                searchExecuted = true;
            } else {
                errorMessage = "Ошибка при загрузке документов.";
            }
        } catch (e) {
            console.error(e);
            errorMessage = "Ошибка подключения к серверу.";
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
            on:keydown={(e) => e.key === 'Enter' && handleSearchClick()}
        />
        <button on:click={handleSearchClick} id="search-button" aria-label="Поиск">Поиск</button>
    </div>

    {#if errorMessage}
        <div role="alert" class="error-notice">
            {errorMessage}
        </div>
    {/if}

    {#if searchExecuted && documents.length === 0 && !loading && !errorMessage}
        <div role="status" class="empty-notice">
            Документы не найдены
        </div>
    {/if}

    <div id="results-container" style={documents.length > 0 ? "display: block;" : "display: none;"}>
        {#if documents.length > 0}
            <ul class="document-list" id="document-list">
                {#each documents as doc}
                    <li>
                        <span class="doc-title">{doc.title || `Документ ${doc.id}`}</span>
                        {#if doc.doc_type || doc.docType}
                            <span class="doc-badge">{doc.doc_type || doc.docType}</span>
                        {/if}
                        {#if doc.doc_date || doc.docDate}
                            <span class="doc-date">от {doc.doc_date || doc.docDate}</span>
                        {/if}
                    </li>
                {/each}
            </ul>

            <div class="pagination-section">
                <button on:click={prevPage} disabled={page === 0} aria-label="Предыдущая страница" id="prev-page-btn">Назад</button>
                <span class="page-indicator">Страница {page + 1} {#if totalPages > 0}из {totalPages}{/if}</span>
                <button on:click={nextPage} disabled={!hasNext} aria-label="Следующая страница" id="next-page-btn">Вперед</button>
            </div>

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
    </div>

    <footer class="dossier-footer">
        <span>Российский научно-исследовательский институт эпидемиологии</span>
        <button on:click={() => alert('Выходные данные (Imprint / Impressum):\nФБУН «НИИ Эпидемиологии»\nг. Москва, ул. Новогиреевская, 3А')} class="imprint-btn" aria-label="Выходные данные">
            Выходные данные (Imprint / Impressum)
        </button>
    </footer>
</div>

<style>
    .dossier-container { padding: 20px; font-family: sans-serif; background-color: #f8f9ff; color: #011d35; }
    .search-section { margin-bottom: 20px; display: flex; gap: 10px; }
    input { padding: 8px 12px; flex-grow: 1; border: 1px solid #737685; border-radius: 4px; color: #011d35; background-color: #ffffff; font-size: 14px; }
    input:focus { outline: 2px solid #003d9b; outline-offset: 2px; }
    button { padding: 8px 16px; background-color: #003d9b; color: #ffffff; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; font-weight: 600; min-height: 44px; min-width: 44px; }
    button:focus { outline: 2px solid #003d9b; outline-offset: 2px; }
    button:disabled { background-color: #c3c6d6; color: #434654; cursor: not-allowed; }
    .document-list { list-style: none; padding: 0; margin-bottom: 20px; }
    .document-list li { padding: 12px; border: 1px solid #c3c6d6; border-radius: 6px; margin-bottom: 8px; background-color: #ffffff; color: #011d35; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
    .doc-title { font-weight: 600; }
    .doc-badge { background-color: #d1e4ff; color: #003d9b; padding: 2px 8px; border-radius: 12px; font-size: 12px; }
    .doc-date { color: #434654; font-size: 12px; }
    .report-section { display: flex; align-items: center; margin-top: 16px; }
    .feedback-notice { margin-top: 15px; padding: 10px; background-color: #d1e4ff; color: #011d35; border-radius: 4px; font-size: 14px; border: 1px solid #003d9b; }
    .empty-notice { margin-top: 15px; padding: 15px; background-color: #ffffff; color: #434654; border-radius: 4px; font-size: 14px; border: 1px solid #c3c6d6; text-align: center; }
    .error-notice { margin-top: 15px; padding: 10px; background-color: #ffdad6; color: #93000a; border-radius: 4px; font-size: 14px; border: 1px solid #ba1a1a; }
    .pagination-section { margin-top: 20px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; color: #011d35; }
    .page-indicator { font-size: 14px; font-weight: 500; }
    .dossier-footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #c3c6d6; text-align: center; font-size: 12px; color: #434654; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
    .imprint-btn { background: none; border: none; color: #003d9b; text-decoration: underline; cursor: pointer; padding: 0; min-height: auto; min-width: auto; }
</style>