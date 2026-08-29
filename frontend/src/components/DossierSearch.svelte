<script>
    export let apiBaseUrl = "/api/v1";

    let surname = "";
    let documents = [];
    let isSearching = false;
    let loading = false;
    let feedback = "";
    let searchError = "";

    async function searchDossier() {
        if (!surname.trim()) return;
        isSearching = true;
        searchError = "";
        feedback = "";
        try {
            const queryParam = encodeURIComponent(surname.trim());
            const response = await fetch(`${apiBaseUrl}/dossier/documents?employee_id=${queryParam}&query=${queryParam}`);
            if (!response.ok) {
                throw new Error(`Ошибка сервера (${response.status}): Не удалось загрузить документы досье.`);
            }
            const data = await response.json();
            documents = Array.isArray(data) ? data : (data.documents || []);
        } catch (err) {
            documents = [];
            searchError = err.message || "Ошибка подключения при поиске досье.";
        } finally {
            isSearching = false;
        }
    }

    async function generateReport() {
        if (!surname.trim() && documents.length === 0) return;
        loading = true;
        feedback = "";
        try {
            const response = await fetch(`${apiBaseUrl}/dossier/reports`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    employee_id: surname.trim() || "EMP-001",
                    template_type: "SUMMARY_STANDARD",
                    document_ids: documents.map(d => d.id).filter(Boolean)
                })
            });
            if (!response.ok) {
                throw new Error(`Ошибка генерации справки (${response.status})`);
            }
            const data = await response.json();
            feedback = data.summary_text || "✓ Итоговая справка успешно сформирована.";
        } catch (err) {
            feedback = err.message || "Ошибка при генерации справки.";
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
        <button on:click={searchDossier} id="search-button" disabled={isSearching}>
            {isSearching ? 'Поиск...' : 'Поиск'}
        </button>
    </div>

    {#if searchError}
        <div role="alert" class="error-notice">
            {searchError}
        </div>
    {/if}

    {#if documents.length > 0}
        <ul class="document-list" id="document-list">
            {#each documents as doc}
                <li>{doc.title || doc.name}</li>
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
    .dossier-container { padding: 20px; font-family: sans-serif; }
    .search-section { margin-bottom: 20px; display: flex; gap: 10px; }
    input { padding: 8px; flex-grow: 1; border: 1px solid #ccc; border-radius: 4px; }
    button { padding: 8px 16px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; }
    button:disabled { background-color: #a0c4ff; cursor: not-allowed; }
    .document-list { list-style: none; padding: 0; margin-bottom: 20px; }
    .document-list li { padding: 8px; border-bottom: 1px solid #eee; }
    .report-section { display: flex; align-items: center; }
    .error-notice { margin-bottom: 15px; padding: 10px; background-color: #ffdad6; color: #93000a; border-radius: 4px; font-size: 14px; border: 1px solid #ba1a1a; }
    .feedback-notice { margin-top: 15px; padding: 10px; background-color: #d9e3f1; color: #001a40; border-radius: 4px; font-size: 14px; border: 1px solid #003f87; }
    .dossier-footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #eee; text-align: center; font-size: 12px; color: #666; display: flex; justify-content: space-between; align-items: center; }
    .imprint-btn { background: none; border: none; color: #007bff; text-decoration: underline; cursor: pointer; padding: 0; }
</style>
