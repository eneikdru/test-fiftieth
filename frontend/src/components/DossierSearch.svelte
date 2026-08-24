<script>
    let surname = "";
    let documents = [];
    let loading = false;

    function searchDossier() {
        if (!surname.trim()) return;
        documents = [
            { id: 1, title: "Приказ о назначении №42", type: "order" },
            { id: 2, title: "Выписка из учёного совета от 12.05.2023", type: "extract" },
            { id: 3, title: "Отчёт о командировке (Самара)", type: "report" }
        ];
    }

    function generateReport() {
        loading = true;
        setTimeout(() => {
            loading = false;
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
        />
        <button on:click={searchDossier} id="search-button">Поиск</button>
    </div>

    {#if documents.length > 0}
        <ul class="document-list" id="document-list">
            {#each documents as doc}
                <li>{doc.title}</li>
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
</style>
