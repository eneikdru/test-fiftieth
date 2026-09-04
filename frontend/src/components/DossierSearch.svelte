<script>
    let surname = "";
    let documents = [];
    let loading = false;
    let feedback = "";
    let page = 0;
    let size = 10;
    let hasNext = false;
    let period = "all";
    let direction = "all";

    async function searchDossier() {
        if (!surname.trim()) return;
        loading = true;
        try {
            const res = await fetch(`/api/v1/dossier/documents?employee_surname=${encodeURIComponent(surname)}&page=${page}&size=${size}&period=${period}&direction=${direction}`);
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

    function generateReport() {
        loading = true;
        feedback = "";
        setTimeout(() => {
            loading = false;
            feedback = "✓ Итоговая справка успешно сформирована.";

            // Trigger PDF download simulation
            const a = document.createElement('a');
            a.href = 'data:application/pdf;base64,JVBERi0xLjQKJcOkw7zDtsOfCjIgMCBvYmoKPDwvTGVuZ3RoIDMgMCBSL0ZpbHRlci9GbGF0ZURlY29kZT4+CnN0cmVhbQp4nDPQM1Qo5ypUMFAwALJMLY31jBQK04sSSxIVMvNzlXADFhB2uQplVQrlV1cUK2TmphZoOufn5xclFqUml2Tm5ykk5+elFhQrFJanFuWnKmTmJ6ZkZucpOOeX5OYlp6YWZabml2SmKhTnF+WklmTmpxaVFOYllqQWK5SlpqUqZGbmpQDFMjLzUoFimZl5KQqZJXmJmXkKzgX5RSUKOal5JZkKmcn5eVwF+UX5yRkK+fkp5Ym5iZk5qblJqQpGhgYKEB8DQx0F+0IFR0cFR6DEmFhdxzQlx6TEFJAyUwVQ/tWpXIUKtgoZibmpxUoGBnADjAwMjAwMFEpSi4sVkjLz0hVy8lMKS1IVchLLU1KLFIz0DEyMDGz0DM3NDQyUDFR0FfLz04sSc0tKFAwNDMwMDFQ0FQz0DAxMdBTyU4pTi1R0FErzc1LzFfLLFHLB5tTVK3h5+QMA7YtWwA0KZW5kc3RyZWFtCmVuZG9iagoKCjMgMCBvYmoKNDI4CmVuZG9iagoKMSAwIG9iago8PC9UeXBlL1BhZ2UvTWVkaWFCb3ggWzAgMCA1OTUgODQyXQovUmVzb3VyY2VzPDwvRm9udDw8L0YxIDQgMCBSPj4+PgovQ29udGVudHMgMiAwIFIKL1BhcmVudCA1IDAgUgo+PgplbmRvYmoKCjQgMCBvYmoKPDwvVHlwZS9Gb250L1N1YnR5cGUvVHlwZTEvQmFzZUZvbnQvSGVsdmV0aWNhPj4KZW5kb2JqCgo1IDAgb2JqCjw8L1R5cGUvUGFnZXMvQ291bnQgMS9LaWRzWzEgMCBSXT4+CmVuZG9iagoKNiAwIG9iago8PC9UeXBlL0NhdGFsb2cvUGFnZXMgNSAwIFI+PgplbmRvYmoKCjcgMCBvYmoKPDwvUHJvZHVjZXIoZ2F1c3NfaW5pdF8yMDIzKS9DcmVhdGlvbkRhdGUoRDoyMDIzMDUyNDE0MjUzMlopL01vZERhdGUoRDoyMDIzMDUyNDE0MjUzMlopPj4KZW5kb2JqCgp4cmVmCjAgOAowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDA1MjUgMDAwMDAgbiAKMDAwMDAwMDAxOSAwMDAwMCBuIAowMDAwMDAwNTA1IDAwMDAwIG4gCjAwMDAwMDA2MzUgMDAwMDAgbiAKMDAwMDAwMDcyMyAwMDAwMCBuIAowMDAwMDAwNzgxIDAwMDAwIG4gCjAwMDAwMDA4MzAgMDAwMDAgbiAKdHJhaWxlcgo8PC9TaXplIDgvUm9vdCA2IDAgUi9JbmZvIDcgMCBSL0lEIFs8MTQzQ0Y3MUE5QTM0NTE4NjhBNENCODMwREYyMjQyQTM+PDE0M0NGNzFBOUEzNDUxODY4QTRDQjgzMERGMjI0MkEzPl0+PgpzdGFydHhyZWYKOTQ3CiUlRU9GCg==';
            a.download = 'dossier-report.pdf';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
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
        <select bind:value={period} id="filter-period" aria-label="Период">
            <option value="all">За все время</option>
            <option value="last_month">Конкретный месяц</option>
            <option value="last_3_months">Несколько месяцев (3)</option>
            <option value="last_year">Полный календарный год</option>
        </select>
        <select bind:value={direction} id="filter-direction" aria-label="Научное направление">
            <option value="all">Все направления</option>
            <option value="virology">Вирусология</option>
            <option value="bacteriology">Бактериология</option>
            <option value="epidemiology">Эпиднадзор</option>
            <option value="vaccinology">Вакцинопрофилактика</option>
        </select>
        <button on:click={handleSearchClick} id="search-button" aria-label="Поиск">Поиск</button>
    </div>

    {#if documents.length > 0}
        <ul class="document-list" id="document-list">
            {#each documents as doc}
                <li>{doc.title || `Документ ${doc.id}`}</li>
            {/each}
        </ul>
        <div class="report-section">
            <button on:click={generateReport} id="generate-report-button" aria-label="Сформировать итоговую справку" disabled={loading} class="export-button">
                {#if loading}
                    <span id="loading-spinner">Загрузка...</span>
                {:else}
                    <span class="material-symbols-outlined icon">download</span>
                    Сформировать итоговую справку
                {/if}
            </button>
        </div>
        {#if feedback}
            <div role="status" aria-live="polite" class="feedback-notice" id="feedback-notice">
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
    .dossier-container { padding: 1.25rem; font-family: sans-serif; }

    .search-section {
        margin-bottom: 1.25rem;
        display: flex;
        flex-direction: column;
        gap: 0.625rem;
    }

    @media (min-width: 768px) {
        .search-section {
            flex-direction: row;
        }
    }

    input, select { padding: 0.5rem; border: 1px solid #767676; border-radius: 0.25rem; color: #111; }
    input { flex-grow: 1; }

    input:focus, select:focus { outline: 2px solid #005fcc; outline-offset: 2px; }

    button { padding: 0.5rem 1rem; background-color: #005fcc; color: white; border: none; border-radius: 0.25rem; cursor: pointer; }
    button:focus { outline: 2px solid #005fcc; outline-offset: 2px; }
    button:disabled { background-color: #99c2ff; color: #333; cursor: not-allowed; }

    .document-list { list-style: none; padding: 0; margin-bottom: 1.25rem; }
    .document-list li { padding: 0.5rem; border-bottom: 1px solid #767676; color: #111; }

    .report-section { display: flex; align-items: center; justify-content: flex-end; }

    .export-button {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        font-weight: bold;
    }

    .export-button .icon {
        font-size: 1.2em;
    }

    @media (max-width: 767px) {
        .report-section {
            justify-content: center;
            position: fixed;
            bottom: 1.5rem;
            right: 1.5rem;
            z-index: 50;
        }

        .export-button {
            width: 3.5rem;
            height: 3.5rem;
            border-radius: 50%;
            padding: 0;
            justify-content: center;
            box-shadow: 0 0.25rem 0.375rem rgba(0,0,0,0.3);
        }

        .export-button .icon {
            font-size: 1.5rem;
        }

        .export-button span:not(.icon) {
            display: none;
        }
    }

    .feedback-notice { margin-top: 1rem; padding: 0.625rem; background-color: #d9e3f1; color: #001a40; border-radius: 0.25rem; font-size: 0.875rem; border: 1px solid #003f87; }
    .pagination-section { margin-top: 1.25rem; display: flex; justify-content: space-between; align-items: center; color: #111; }
    .dossier-footer { margin-top: 2.5rem; padding-top: 1.25rem; border-top: 1px solid #767676; text-align: center; font-size: 0.75rem; color: #333; display: flex; justify-content: space-between; align-items: center; }
    .imprint-btn { background: none; border: none; color: #005fcc; text-decoration: underline; cursor: pointer; padding: 0; }
</style>
