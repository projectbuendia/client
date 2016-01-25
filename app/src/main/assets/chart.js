function isRowEmpty(parentNode) {
  var rowEmpty = true;
  $(parentNode).find('td').each(function(index, element) {
    var innerHtml = element.innerHTML.trim();
    if ( innerHtml != "" ) {
      rowEmpty = false;
    }
  });
  return rowEmpty;
}

function popup(name, pairs) {
  // var dialog = document.getElementById('obs-dialog');
  // var html = '<h2>' + name + '</h2>';
  var text = name + '\n\n';
  if (pairs.length > 0) {
    for (var i = 0; i < pairs.length; i++) {
      var value = pairs[i][0], time = pairs[i][1];
      // html += '<b>' + value + '</b> at ' + time + '<br>';
      text += time + ' – ' + value + '\n';
    }
    // html += '&nbsp;<br>Comments <input type="text" size="40">';
  } else {
    // html += 'No observations.';
    text += 'No observations.';
  }
  alert(text);
  return;
  // dialog.innerHTML = html;
  // dialog.showModal();
}

function runTileScript(pointGroupsByConceptId, conceptIds, tileScript) {
  conceptIds = conceptIds.split(',');
  var args = [conceptIds];
  for (var i = 0; i < conceptIds.length; i++) {
    var id = conceptIds[i];
    args.push(getPoints(pointGroupsByConceptId[id]));
  }
  applyScript(tileScript, args);
}

function runChartRowScript(pointGroupsByConceptId, conceptIds, chartRowScript) {
  conceptIds = conceptIds.split(',');
  var args = [conceptIds];
  for (var i = 0; i < conceptIds.length; i++) {
    var id = conceptIds[i];
    var pointGroups = pointGroupsByConceptId[id];
    for (var j = 0; j < pointGroups.length; j++) {
      var group = pointGroups[j];
      group.cell = document.getElementById('cell-' + id + '-' + group.start);
    }
    args.push(pointGroups);
  }
  applyScript(chartRowScript, args);
}

function applyScript(script, args) {
  var code = '(' + script + ')';
  try {
    // Use eval so that bad syntax in one script doesn't break the whole page.
    var func = eval(code);  // should evaluate to a function
    func.apply(null, args);
  } catch (e) {
    console.log(e);
    console.log(e.stack + '\nwhile trying to run script:\n' + code);
  }
}

function getPoints(pointGroups) {
  var pointArrays = [];
  for (var i = 0; i < pointGroups.length; i++) {
    pointArrays.push(pointGroups[i].points);
  }
  return Array.prototype.concat.apply([], pointArrays);
}
