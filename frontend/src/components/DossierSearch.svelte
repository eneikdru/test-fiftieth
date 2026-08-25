<script>
    export let apiBaseUrl = '/api/v1/dossier';

    let surname = "";
    let documents = [];
    let loading = false;
    let searching = false;
    let reportResult = null;
    let errorMessage = null;

    async function searchDossier() {
        if (!surname.trim()) return;
        searching = true;
        errorMessage = null;
        try {
            const params = new URLSearchParams();
            params.append('employee_surname', surname.trim());
            let res = await fetch(`${apiBaseUrl}/documents?${params.toString()}`);
            if (!res.ok) {
                res = await fetch(`${apiBaseUrl}/documents?query=${encodeURIComponent(surname.trim())}`);
            }
            if (res.ok) {
                const data = await res.json();
                documents = Array.isArray(data) ? data : [];
            } else {
                errorMessage = "Не удалось загрузить документы";
            }
        } catch (err) {
            errorMessage = "Ошибка подключения к серверу";
        } finally {
            searching = false;
        }
    }

    async function generateReport() {
        if (loading) return;
        loading = true;
        errorMessage = null;
        try {
            const empId = documents.length > 0 && documents[0].employeeId
                ? documents[0].employeeId
                : (surname.trim() || 'EMP-UNKNOWN');
            const docIds = documents.map(d => d.id).filter(id => id != null);

            const res = await fetch(`${apiBaseUrl}/reports`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    employee_id: empId,
                    template_type: 'SUMMARY_STANDARD',
                    document_ids: docIds.length > 0 ? docIds : undefined
                })
            });

            if (res.ok) {
                reportResult = await res.json();
            } else {
                errorMessage = "Не удалось сформировать итоговую справку";
            }
        } catch (err) {
            errorMessage = "Ошибка при запросе формирования справки";
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
            id="search-query-input"
        />
        <button on:click={searchDossier} id="search-button" disabled={searching}>
            {searching ? 'Поиск...' : 'Поиск'}
        </button>
    </div>

    {#if errorMessage}
        <div class="error-message" role="alert">{errorMessage}</div>
    {/if}

    {#if documents.length > 0}
        <ul class="document-list" id="document-list">
            {#each documents as doc}
                <li>{doc.title || doc.details || 'Документ без названия'}</li>
            {/each}
        </ul>
        <div class="report-section">
            <button on:click={generateReport} id="generate-report-button" disabled={loading}>
                {#if loading}
                    <span id="loading-spinner">Загрузка...</span>
                {:else}
                    Сформировать итоговую справку
                {/if}
            </button>
        </div>
    {/if}

    {#if reportResult}
        <div class="report-result-card" id="report-result">
            <h3>Итоговая справка сформирована</h3>
            <p>{reportResult.summary_text || 'Справка сгенерирована.'}</p>
            {#if reportResult.download_url}
                <a href={reportResult.download_url} target="_blank" download class="download-link">Скачать справку (PDF)</a>
            {/if}
        </div>
    {/if}
</div>

<style>
    .dossier-container { padding: 20px; font-family: sans-serif; max-width: 800px; margin: 0 auto; }
    .search-section { margin-bottom: 20px; display: flex; gap: 10px; }
    input { padding: 8px 12px; flex-grow: 1; border: 1px solid #ccc; border-radius: 4px; font-size: 14px; }
    button { padding: 8px 16px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; font-size: 14px; }
    button:disabled { background-color: #a0c4ff; cursor: not-allowed; }
    .document-list { list-style: none; padding: 0; margin-bottom: 20px; }
    .document-list li { padding: 10px; border-bottom: 1px solid #eee; background-color: #fff; border-radius: 4px; margin-bottom: 6px; }
    .report-section { display: flex; align-items: center; margin-bottom: 20px; }
    .error-message { color: #d9534f; margin-bottom: 15px; font-size: 14px; }
    .report-result-card { background: #e9f5ff; border: 1px solid #b8daff; padding: 15px; border-radius: 6px; margin-top: 15px; }
    .report-result-card h3 { margin-top: 0; color: #004085; font-size: 16px; }
    .download-link { display: inline-block; margin-top: 8px; color: #0056b3; font-weight: bold; text-decoration: underline; }
</style>
