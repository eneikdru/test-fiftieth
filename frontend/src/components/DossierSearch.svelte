<script>
    let surname = "";
    let documents = [];
    let loading = false;
    let feedback = "";
    let page = 0;
    let size = 10;
    let hasNext = false;

    async function searchDossier() {
        if (!surname.trim()) return;
        loading = true;
        try {
            const res = await fetch(`/api/v1/dossier/documents?employee_surname=${encodeURIComponent(surname)}&page=${page}&size=${size}`);
            if (res.ok) {
                const data = await res.json();
                documents = data;
                hasNext = data.length === size; // simple logic for next page
            }
        } catch (e) {
            console.error(e);
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

    async function generateReport() {
        loading = true;
        feedback = "";
        try {
            const empId = (documents.length > 0 && (documents[0].employee_id || documents[0].employeeId))
                ? (documents[0].employee_id || documents[0].employeeId)
                : (surname.trim() || "EMP-DEFAULT");

            const res = await fetch('/api/v1/dossier/reports', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    employee_id: empId,
                    template_type: 'SUMMARY'
                })
            });

            if (res.ok) {
                const data = await res.json();
                feedback = data.summary_text ? `✓ ${data.summary_text}` : '✓ Итоговая справка успешно сформирована.';
            } else {
                const errData = await res.json().catch(() => ({}));
                feedback = `Ошибка формирования справки: ${errData.message || res.statusText}`;
            }
        } catch (e) {
            console.error(e);
            feedback = "Ошибка сети при формировании справки.";
        } finally {
            loading = false;
        }
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

    {#if documents.length > 0}
        <ul class="document-list" id="document-list">
            {#each documents as doc}
                <li>{doc.title || `Документ ${doc.id}`}</li>
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
    .dossier-container { padding: 20px; font-family: sans-serif; }
    .search-section { margin-bottom: 20px; display: flex; gap: 10px; }
    input { padding: 8px; flex-grow: 1; border: 1px solid #767676; border-radius: 4px; color: #111; }
    input:focus { outline: 2px solid #005fcc; outline-offset: 2px; }
    button { padding: 8px 16px; background-color: #005fcc; color: white; border: none; border-radius: 4px; cursor: pointer; }
    button:focus { outline: 2px solid #005fcc; outline-offset: 2px; }
    button:disabled { background-color: #99c2ff; color: #333; cursor: not-allowed; }
    .document-list { list-style: none; padding: 0; margin-bottom: 20px; }
    .document-list li { padding: 8px; border-bottom: 1px solid #767676; color: #111; }
    .report-section { display: flex; align-items: center; }
    .feedback-notice { margin-top: 15px; padding: 10px; background-color: #d9e3f1; color: #001a40; border-radius: 4px; font-size: 14px; border: 1px solid #003f87; }
    .pagination-section { margin-top: 20px; display: flex; justify-content: space-between; align-items: center; color: #111; }
    .dossier-footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #767676; text-align: center; font-size: 12px; color: #333; display: flex; justify-content: space-between; align-items: center; }
    .imprint-btn { background: none; border: none; color: #005fcc; text-decoration: underline; cursor: pointer; padding: 0; }
</style>