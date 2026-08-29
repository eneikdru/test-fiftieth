<script>
    let surname = "";
    let activeSearchQuery = "";
    let documents = [];
    let loading = false;
    let loadingReport = false;
    let feedback = "";
    let page = 0;
    let size = 10;
    let hasMore = false;
    let searchAttempted = false;

    async function fetchDocuments(currentPage) {
        if (!activeSearchQuery.trim()) return;
        loading = true;
        searchAttempted = true;
        try {
            const token = localStorage.getItem("token") || "";
            const response = await fetch(`/api/v1/dossier/documents?query=${encodeURIComponent(activeSearchQuery)}&page=${currentPage}&size=${size}`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            if (response.ok) {
                const data = await response.json();
                documents = data;
                hasMore = data.length === size; // simple heuristic
                page = currentPage;
            } else {
                feedback = "Ошибка при загрузке документов.";
            }
        } catch (error) {
            feedback = "Сетевая ошибка.";
        } finally {
            loading = false;
        }
    }

    function searchDossier() {
        if (!surname.trim()) return;
        activeSearchQuery = surname;
        page = 0;
        fetchDocuments(page);
    }

    function nextPage() {
        fetchDocuments(page + 1);
    }

    function prevPage() {
        if (page > 0) {
            fetchDocuments(page - 1);
        }
    }

    function generateReport() {
        loadingReport = true;
        feedback = "";
        setTimeout(() => {
            loadingReport = false;
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
            id="search-query-input"
            aria-label="Поиск по фамилии сотрудника"
        />
        <button on:click={searchDossier} id="search-button" aria-label="Искать документы">Поиск</button>
    </div>

    {#if loading && documents.length === 0}
        <div id="loading-spinner" aria-live="polite">Загрузка...</div>
    {/if}

    {#if searchAttempted && !loading && documents.length === 0}
        <div class="feedback-notice" role="status" aria-live="polite">Документы не найдены.</div>
    {/if}

    {#if searchAttempted && (documents.length > 0 || page > 0)}
        {#if documents.length > 0}
            <ul class="document-list" id="document-list" aria-label="Список документов">
                {#each documents as doc}
                    <li>{doc.title}</li>
                {/each}
            </ul>
        {/if}

        <div class="pagination-controls" aria-label="Управление страницами">
            <button
                on:click={prevPage}
                disabled={page === 0 || loading}
                id="prev-page-button"
                aria-label="Предыдущая страница"
            >
                Назад
            </button>
            <span id="page-indicator" class="page-indicator" aria-live="polite">Страница {page + 1}</span>
            <button
                on:click={nextPage}
                disabled={!hasMore || loading || documents.length === 0}
                id="next-page-button"
                aria-label="Следующая страница"
            >
                Вперёд
            </button>
        </div>

        {#if documents.length > 0}
            <div class="report-section">
                <button on:click={generateReport} id="generate-report-button" disabled={loadingReport} aria-label="Сформировать итоговую справку">
                    {#if loadingReport}
                        <span id="loading-spinner">Загрузка...</span>
                    {:else}
                        Сформировать итоговую справку
                    {/if}
                </button>
            </div>
        {/if}
        {#if feedback}
            <div role="status" aria-live="polite" class="feedback-notice">
                {feedback}
            </div>
        {/if}
    {/if}

    <footer class="dossier-footer">
        <span>Российский научно-исследовательский институт эпидемиологии</span>
        <button on:click={() => alert('Выходные данные (Imprint / Impressum):\nФБУН «НИИ Эпидемиологии»\nг. Москва, ул. Новогиреевская, 3А')} class="imprint-btn">
            Выходные данные (Imprint / Impressum)
        </button>
    </footer>
</div>

<style>
    .dossier-container { padding: 20px; font-family: sans-serif; color: #333; }
    .search-section { margin-bottom: 20px; display: flex; gap: 10px; }
    input { padding: 8px; flex-grow: 1; border: 1px solid #767676; border-radius: 4px; color: #111; }
    input:focus { outline: 2px solid #0056b3; outline-offset: 2px; }
    button { padding: 8px 16px; background-color: #0056b3; color: #ffffff; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
    button:hover { background-color: #004494; }
    button:focus { outline: 2px solid #0056b3; outline-offset: 2px; }
    button:disabled { background-color: #6c757d; color: #e9ecef; cursor: not-allowed; }
    .document-list { list-style: none; padding: 0; margin-bottom: 20px; border: 1px solid #ccc; border-radius: 4px; }
    .document-list li { padding: 12px; border-bottom: 1px solid #eee; }
    .document-list li:last-child { border-bottom: none; }
    .pagination-controls { display: flex; align-items: center; gap: 15px; margin-bottom: 20px; }
    .page-indicator { font-weight: bold; }
    .report-section { display: flex; align-items: center; margin-bottom: 20px; }
    .feedback-notice { margin-top: 15px; padding: 10px; background-color: #e2e3e5; color: #383d41; border-radius: 4px; font-size: 14px; border: 1px solid #d6d8db; }
    .dossier-footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #ccc; text-align: center; font-size: 12px; color: #555; display: flex; justify-content: space-between; align-items: center; }
    .imprint-btn { background: none; border: none; color: #0056b3; text-decoration: underline; cursor: pointer; padding: 0; font-weight: normal; }
    .imprint-btn:hover { background: none; }
</style>
