$('#csrf').value = cookie('XSRF-TOKEN');
if (location.search.includes('error')) $('#err').classList.remove('hidden');
