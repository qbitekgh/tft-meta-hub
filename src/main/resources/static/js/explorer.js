// Small progressive enhancements of the explorer page; everything also works without JavaScript.
(function () {
    'use strict';

    // Selects marked with data-autosubmit send their form as soon as the value changes.
    document.querySelectorAll('[data-autosubmit]').forEach(function (field) {
        field.addEventListener('change', function () {
            field.form.submit();
        });
    });

    // Clicking anywhere on a table row adds the row's element as a filter (the name link does the same).
    document.querySelectorAll('.stats-table__row[data-href]').forEach(function (row) {
        row.addEventListener('click', function (event) {
            if (event.target.closest('a')) {
                return;
            }
            window.location.href = row.dataset.href;
        });
    });

    // The search box hides rows whose name does not contain the typed text. It only filters what is already
    // on the page, so it needs no request to the server.
    var search = document.querySelector('[data-table-search]');
    if (search) {
        var noMatch = document.querySelector('.stats-table__no-match');
        search.addEventListener('input', function () {
            var text = search.value.trim().toLowerCase();
            var visible = 0;
            document.querySelectorAll('.stats-table__row').forEach(function (row) {
                var matches = row.dataset.name.indexOf(text) !== -1;
                row.hidden = !matches;
                if (matches) {
                    visible++;
                }
            });
            if (noMatch) {
                noMatch.hidden = visible > 0 || document.querySelectorAll('.stats-table__row').length === 0;
            }
        });
        // Enter in the search box must not submit the table settings form
        search.addEventListener('keydown', function (event) {
            if (event.key === 'Enter') {
                event.preventDefault();
            }
        });
    }
})();
