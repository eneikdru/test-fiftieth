<script>
    import { fade } from 'svelte/transition';

    let period = 'last30';
    let direction = 'all';

    let documents = [];

    let isDownloading = false;
    let downloadNotice = '';

    const DOWNLOAD_DELAY_MS = 1500;

    const allDocuments = [
        { id: 1, title: 'Q3 Virology Report', direction: 'virology', period: 'last30', status: 'Approved', statusClass: 'bg-surface-container-high text-primary border-outline-variant' },
        { id: 2, title: 'Bacteriology Findings', direction: 'bacteriology', period: 'last90', status: 'Draft', statusClass: 'bg-surface-variant text-on-surface-variant border-outline' },
        { id: 3, title: 'Epidemiology Audit 2023', direction: 'epidemiology', period: 'year', status: 'Approved', statusClass: 'bg-surface-container-high text-primary border-outline-variant' },
        { id: 4, title: 'Vaccine Rollout Plan', direction: 'vaccinology', period: 'last30', status: 'Draft', statusClass: 'bg-surface-variant text-on-surface-variant border-outline' }
    ];

    $: documents = allDocuments.filter(doc => {
        const periodMatch = period === 'all' || doc.period === period;
        const directionMatch = direction === 'all' || doc.direction === direction;
        return periodMatch && directionMatch;
    });

    function handleDownload() {
        isDownloading = true;
        downloadNotice = '';
        setTimeout(() => {
            isDownloading = false;
            downloadNotice = '✓ Dossier successfully generated as PDF and downloaded.';

            const blob = new Blob(['Dummy PDF Content'], { type: 'application/pdf' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = 'dossier.pdf';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            URL.revokeObjectURL(url);
        }, DOWNLOAD_DELAY_MS);
    }
</script>

<div class="bg-background text-on-background min-h-screen font-body-lg p-container-margin pb-[100px]">

    <header class="flex items-center justify-between mb-stack-lg min-h-[44px]">
        <h1 class="font-headline-md text-headline-md text-primary">Dossier Builder</h1>
    </header>

    <main class="flex flex-col gap-stack-lg">
        <section class="flex flex-col gap-stack-md bg-surface p-stack-md border border-outline-variant rounded-DEFAULT">
            <h2 class="font-headline-sm text-headline-sm text-primary mb-stack-sm">Filters</h2>
            <div class="flex flex-wrap gap-stack-md">
                <div class="flex flex-col gap-unit">
                    <label for="period" class="font-label-mono text-label-mono text-on-surface-variant">Period</label>
                    <select id="period" bind:value={period} class="min-h-[44px] min-w-[200px] bg-surface rounded-DEFAULT border border-outline-variant px-4 py-2 text-on-surface focus:border-secondary focus:ring-1 focus:ring-secondary outline-none transition-all cursor-pointer">
                        <option value="all">Any time</option>
                        <option value="last30">Last 30 Days</option>
                        <option value="last90">Last 90 Days</option>
                        <option value="year">Full Year</option>
                    </select>
                </div>

                <div class="flex flex-col gap-unit">
                    <label for="direction" class="font-label-mono text-label-mono text-on-surface-variant">Scientific Direction</label>
                    <select id="direction" bind:value={direction} class="min-h-[44px] min-w-[200px] bg-surface rounded-DEFAULT border border-outline-variant px-4 py-2 text-on-surface focus:border-secondary focus:ring-1 focus:ring-secondary outline-none transition-all cursor-pointer">
                        <option value="all">All Directions</option>
                        <option value="virology">Virology</option>
                        <option value="bacteriology">Bacteriology</option>
                        <option value="epidemiology">Epidemiology</option>
                        <option value="vaccinology">Vaccinology</option>
                    </select>
                </div>
            </div>
        </section>

        <section class="flex flex-col gap-gutter">
            {#if documents.length === 0}
                <div class="p-stack-md bg-surface border border-outline-variant rounded-DEFAULT text-on-surface-variant text-center">
                    No documents match the selected filters.
                </div>
            {:else}
                <div class="grid grid-cols-1 md:grid-cols-2 gap-gutter" role="list">
                    {#each documents as doc (doc.id)}
                        <article role="listitem" class="bg-surface border border-outline-variant rounded-DEFAULT p-stack-md flex flex-col gap-stack-sm transition-shadow hover:shadow-md">
                            <div class="flex justify-between items-start gap-2">
                                <h3 class="font-headline-sm text-headline-sm text-primary break-words">{doc.title}</h3>
                                <span class="px-3 py-1 font-label-mono text-label-mono rounded-full border {doc.statusClass} whitespace-nowrap">
                                    {doc.status}
                                </span>
                            </div>
                            <div class="flex items-center gap-4 mt-auto pt-stack-sm border-t border-outline-variant">
                                <div class="font-label-mono text-label-mono text-outline uppercase">
                                    {doc.direction}
                                </div>
                            </div>
                        </article>
                    {/each}
                </div>
            {/if}
        </section>

        <section class="mt-stack-md flex flex-col items-center md:items-start gap-stack-sm">
            <button
                on:click={handleDownload}
                disabled={isDownloading || documents.length === 0}
                class="min-h-[44px] px-6 py-2 bg-secondary text-on-primary font-body-md font-bold rounded-DEFAULT flex items-center justify-center gap-2 transition-colors focus:ring-2 focus:ring-offset-2 focus:ring-secondary disabled:opacity-50 disabled:cursor-not-allowed hover:bg-secondary-container"
                aria-busy={isDownloading}
            >
                {#if isDownloading}
                    <span class="material-symbols-outlined animate-spin" aria-hidden="true">sync</span>
                    Generating PDF...
                {:else}
                    <span class="material-symbols-outlined" aria-hidden="true">download</span>
                    Export Dossier
                {/if}
            </button>

            {#if downloadNotice}
                <div role="status" aria-live="polite" transition:fade class="p-3 bg-surface-container-high text-on-surface rounded-DEFAULT border border-outline-variant text-body-md mt-2">
                    {downloadNotice}
                </div>
            {/if}
        </section>
    </main>
</div>
