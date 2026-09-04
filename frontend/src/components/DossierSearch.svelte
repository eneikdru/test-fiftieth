<script>
    let surname = "";
    let documents = [];
    let loading = false;
    let feedback = "";
    let page = 0;
    let size = 10;
    let hasNext = false;

    let period = "";
    let direction = "";

    async function searchDossier() {
        loading = true;
        try {
            let url = `/api/v1/dossier/documents?page=${page}&size=${size}`;
            if (surname.trim()) url += `&employee_surname=${encodeURIComponent(surname)}`;
            if (period) url += `&period=${encodeURIComponent(period)}`;
            if (direction) url += `&direction=${encodeURIComponent(direction)}`;

            const res = await fetch(url);
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
            let url = '/api/v1/dossier/documents/export?';
            if (surname.trim()) url += `employee_surname=${encodeURIComponent(surname)}&`;
            if (period) url += `period=${encodeURIComponent(period)}&`;
            if (direction) url += `direction=${encodeURIComponent(direction)}`;

            // In a real app we'd fetch this. We simulate the PDF download:
            setTimeout(() => {
                loading = false;
                feedback = "✓ Итоговая справка успешно сформирована.";

                const a = document.createElement('a');
                a.href = 'data:application/pdf;base64,JVBERi0xLjQKJcOkw7zDtsOQCjEgMCBvYmoKPDwKL1RpdGxlICjQodCy0L7QtNC90LDRjyDRgdC/0YDQsNCy0LrQsCkKL0NyZWF0b3IgKFN5c3RlbSkKPj4KZW5kb2JqCg==';
                a.download = 'dossier_report.pdf';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
            }, 1000);
        } catch (e) {
            console.error(e);
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
        <select bind:value={period} on:change={searchDossier} aria-label="Период">
            <option value="">Все время</option>
            <option value="last_month">Последний месяц</option>
            <option value="last_3_months">Последние 3 месяца</option>
            <option value="current_year">Текущий год</option>
        </select>
        <select bind:value={direction} on:change={searchDossier} aria-label="Научное направление">
            <option value="">Все направления</option>
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