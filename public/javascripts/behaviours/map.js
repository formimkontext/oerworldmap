if (!Array.prototype.filter) {
  Array.prototype.filter = function(fun/*, thisArg*/) {
    'use strict';

    if (this === void 0 || this === null) {
      throw new TypeError();
    }

    var t = Object(this);
    var len = t.length >>> 0;
    if (typeof fun !== 'function') {
      throw new TypeError();
    }

    var res = [];
    var thisArg = arguments.length >= 2 ? arguments[1] : void 0;
    for (var i = 0; i < len; i++) {
      if (i in t) {
        var val = t[i];

        // NOTE: Technically this should Object.defineProperty at
        //       the next index, as push can be affected by
        //       properties on Object.prototype and Array.prototype.
        //       But that method's new, and collisions should be
        //       rare, so use the more-compatible alternative.
        if (fun.call(thisArg, val, i, t)) {
          res.push(val);
        }
      }
    }

    return res;
  };
}

var test_heat_data_dump = {"@type":"Aggregation","@id":"country-list","entries":[{"value":37,"key":"US"},{"value":34,"key":"DE"},{"value":21,"key":"GB"},{"value":16,"key":"CA"},{"value":15,"key":"ES"},{"value":13,"key":"RU"},{"value":11,"key":"AU"},{"value":8,"key":"ZA"},{"value":7,"key":"BR"},{"value":6,"key":"IN"},{"value":6,"key":"IT"},{"value":4,"key":"BE"},{"value":4,"key":"MX"},{"value":4,"key":"PL"},{"value":4,"key":"UA"},{"value":3,"key":"CH"},{"value":3,"key":"EC"},{"value":3,"key":"FR"},{"value":3,"key":"GR"},{"value":3,"key":"IE"},{"value":3,"key":"JP"},{"value":3,"key":"NL"},{"value":3,"key":"NZ"},{"value":3,"key":"PT"},{"value":3,"key":"RO"},{"value":2,"key":"AL"},{"value":2,"key":"HR"},{"value":2,"key":"SE"},{"value":1,"key":"AM"},{"value":1,"key":"AT"},{"value":1,"key":"AW"},{"value":1,"key":"BH"},{"value":1,"key":"CL"},{"value":1,"key":"CN"},{"value":1,"key":"CO"},{"value":1,"key":"CZ"},{"value":1,"key":"DM"},{"value":1,"key":"EE"},{"value":1,"key":"FI"},{"value":1,"key":"FJ"},{"value":1,"key":"GE"},{"value":1,"key":"HU"},{"value":1,"key":"ID"},{"value":1,"key":"IR"},{"value":1,"key":"IS"},{"value":1,"key":"KE"},{"value":1,"key":"KR"},{"value":1,"key":"LB"},{"value":1,"key":"LT"},{"value":1,"key":"MY"},{"value":1,"key":"NA"},{"value":1,"key":"OM"},{"value":1,"key":"PA"},{"value":1,"key":"RS"},{"value":1,"key":"SI"},{"value":1,"key":"SN"},{"value":1,"key":"TG"},{"value":1,"key":"TH"},{"value":1,"key":"TN"},{"value":1,"key":"TR"},{"value":1,"key":"TT"},{"value":1,"key":"UY"},{"value":1,"key":"VE"}]};

Hijax.behaviours.map = {
  
  heat_data : false,
  color : false,
  
  throttledTimer : false,
  
  width : false,
  height : false,
  
  zoom : false,
  
  topo : false,
  projection : false,
  path : false,
  svg : false,
  g : false,
  
  placemarks : [],
  tooltip : false,

  initialized : false,

  world : false,
  
  attach : function(context) {
    var that = this;
    
    that.context = context;
    
    // return if no map in context
    if(! $('div[data-view="map"]', that.context).length) {
      return;
    }
    
    // create map container
    that.container = $('<div id="map"></div>')[0];
    
    // append to view
    $('div[data-view="map"]').prepend(that.container);
    
    // hide table
    $( that.container ).siblings("table").hide();
    
    // set mapview
    $( that.container ).closest('div[role="main"], div[role="complementary"]').addClass("map-view");
    
    // create tooltip
    that.tooltip = $('<div class="map-tooltip"></div>')[0];
    $(that.container).append(that.tooltip);

    // load map data from session storage
    if (window.sessionStorage && window.sessionStorage.getItem('ne_50m_admin_0_countries_topo')) {
      that.world = JSON.parse(window.sessionStorage.getItem('ne_50m_admin_0_countries_topo'));
    }
     
    that.zoom = d3.behavior.zoom()
      .scaleExtent([1, 100])
      .on("zoom", that.move);
    that.setup();
    that.draw();
    
    $(window).resize(function(){
      that.onResize();
    });
  },
  
  setHeatmapData : function(heat_data) {
    var that = this;
    
    that.heat_data = {};

    for(var i = 0; i < heat_data["entries"].length; i++) {
      that.heat_data[ heat_data["entries"][i].key.toUpperCase() ] = heat_data["entries"][i].value;
    }
    
    d3.select('svg').remove();
    that.setup();
    that.draw();
  },
  
  onResize : function() {
    var that = this;
    
    that.doThrottled(function(){
      d3.select('svg').remove();
      that.setup();
      that.draw();
    });
  },
  
  move : function() {
    var that = Hijax.behaviours.map;
    
    var t = d3.event.translate;
    var s = d3.event.scale;
    var h = that.height / 4;
    
    that.setView(t, s, h);
  },
  
  setView : function(t, s, h) {
    var that = this;
    
    t[0] = Math.min(
      (that.width / that.height) * (s - 1), 
      Math.max(that.width * (1 - s), t[0])
    );

    t[1] = Math.min(
      h * (s - 1) + h * s, 
      Math.max(that.height	 * (1 - s) - h * s, t[1])
    );

    that.zoom.translate(t);
    that.g
      .attr("transform", "translate(" + t + ")scale(" + s + ")");
  
    //adjust the country stroke width based on zoom level
    that.doThrottled(function(){
      d3.selectAll(".country").style("stroke-width", 0.5 / s);
      d3.selectAll(".placemark").style("font-size", 28 / s + "px");
      d3.selectAll(".placemark").style("stroke-width", 0.5 / s);
    });
  },

  doThrottled: function(callback) {
    var that = this;
    
    window.clearTimeout( that.throttledTimer );
    that.throttledTimer = window.setTimeout(function(){
      callback();
    }, 200);
  },
  
  setup : function() {
    var that = this;
    
    that.width = that.container.offsetWidth;
    that.height = that.container.offsetHeight;

    that.projection = d3.geo.miller()
      .translate([
        (that.width / 2),
        (that.height / 1.55)
      ]).scale( that.width / 2 / Math.PI );
    
    that.path = d3.geo.path().projection(that.projection);
    
    that.svg = d3
      .select( "#map" ).append("svg")
      .attr("width", that.width)
      .attr("height", that.height)
      .call( that.zoom )
      .on("click", that.click);
  
    that.g = that.svg.append("g");
  },
  
  draw : function() {
    var that = this;
    
    if(that.topo) {
      that.drawx();
    } else if (that.world) {
      that.topo = topojson.feature(that.world, that.world.objects.ne_50m_admin_0_countries).features;
      that.initialized = true;
      that.drawx();
    } else {
      d3.json("/assets/json/ne_50m_admin_0_countries_topo.json", function(error, world) {
        sessionStorage.setItem('ne_50m_admin_0_countries_topo', JSON.stringify(world));
        that.topo = topojson.feature(world, world.objects.ne_50m_admin_0_countries).features;
        that.drawx();
        that.initialized = true;
      });
    }
  },

  drawx : function() {
    var that = this;
    
    var heats = $.map(that.heat_data, function(value, index){
      return [value];
    });
    
    that.color = d3.scale.log()
      .range(["#a1cd3f", "#eaf0e2"])
      .interpolate(d3.interpolateHcl)
      .domain([d3.quantile(heats, .01), d3.quantile(heats, .99)]);
      
    var country = that.g.selectAll(".country").data( that.topo );
  
    country.enter().insert("path")
      .attr("class", "country")
      .attr("d", that.path)
      .attr("id", function(d,i) { return d.id; })
      .attr("title", function(d,i) { return d.properties.name; })
      .style("fill", function(d,i) {
        if(that.heat_data[ d.id ]) {
          return that.color( that.heat_data[ d.id ] );
        } else {
          return "#ffffff";
        }
      });

    country.on("click", function() {
      if (! d3.event.defaultPrevented) {
        window.location = "/resource/?q=about.\\*.addressCountry:" + this.id;
      }
    });

    country.on("mouseover", function(){
      $(that.tooltip).show();
      $(that.tooltip).html(
        that.getTooltipHtml(this.id, $(this).attr("title"))
      );
    });

    country.on("mousemove", function(){
      $(that.tooltip).css({
          "top": (d3.event.pageY) + "px",
          "left": (d3.event.pageX) + "px"
      });
    });
    
    country.on("mouseout", function(){
      $(that.tooltip).hide();
    });

    that.drawPlacemarks();
    
    that.setBoundingBox();
  },
  
  drawPlacemarks : function() {
    var that = this;
    
    for(i in that.placemarks) {
      that.drawPlacemark(
        that.placemarks[i]["latLng"][0],
        that.placemarks[i]["latLng"][1],
        that.placemarks[i]["url"],
        that.placemarks[i]["name"]
      );
    }
  },
  
  getTooltipHtml : function(id, name) {
    var that = this;

    if (!$.isEmptyObject(that.heat_data)) {
      html =
        (
          that.heat_data[ id ] ?
          '<i class="fa fa-fw fa-user"></i> <strong>' + that.heat_data[ id ] + '</strong> users counted for ' + name :
          '<i class="fa fa-fw fa-user"></i> No users counted for ' + name
        ) + (
          false ?
          '<br><i class="fa fa-fw fa-trophy"></i> And we have a country champion!<br>' :
          ''
        );
    } else {
      html = '<i>' + name + '</i>';
    }
    
    return html;
  },
  
  drawPlacemark : function(lat, lon, url, name) {
    var that = this;
  
    var gpoint = that.g.append("g").attr("class", "gpoint");
    var x = that.projection([lon,lat])[0];
    var y = that.projection([lon,lat])[1];

    gpoint
      .append('a')
      .attr("xlink:href", url)
      .attr("id", url.split("/")[2])
      .attr("xlink:title", name)
      .append('text')
      .attr("x", x)
      .attr("y", y)
      .attr('text-anchor', 'middle')
      .attr("class", "placemark")
      .text('\uf041');
  },
  
  addPlacemarks : function( placemarks ) {
    var that = this;
    
    that.placemarks = that.placemarks.concat( placemarks );
    
    if (that.initialized) {
      for(i in placemarks) {
        that.drawPlacemark(
          placemarks[i]["latLng"][0],
          placemarks[i]["latLng"][1],
          placemarks[i]["url"],
          placemarks[i]["name"]
        );
      }
    }
  },
  
  addPoint : function(lat, lon, text) {
    var that = this;
  
    var gpoint = that.g.append("g").attr("class", "gpoint");
    var x = that.projection([lat,lon])[0];
    var y = that.projection([lat,lon])[1];
  
    gpoint.append("svg:circle")
      .attr("cx", x)
      .attr("cy", y)
      .attr("class","point")
      .attr("r", 1.5);
    
    if(text.length > 0) {	
      gpoint.append("text")
        .attr("x", x + 3)
        .attr("y", y + 3)
        .attr("class", "text")
        .text(text);
    }

  },

  setBoundingBox : function() {

    var that = this;
    var bounds = false;
    var q = Hijax.functions.getQueryVariable("q");
    var id = Hijax.functions.getResourceId();
    
    if (q) {
      var countryParams = q.match(/(addressCountry:..)/g);
      if (countryParams) {
        var countries = countryParams.map(function(country) {
          return country.split(':')[1];
        });
        var features = that.topo.filter(function(feature) {
          if (countries.indexOf(feature.id) != -1) {
            return feature;
          }
        });
        
        var feature = features[0];
        
        if( feature.id == "RU" ) {
          bounds = [that.projection([32.459718, 73.276382]), that.projection([175.545652, 41.637556])];
        } else if ( feature.id == "US" ) { 
          bounds = [that.projection([-133.457967, 51.442811]), that.projection([-65.079064, 24.590736])];
        } else if ( feature.id == "FR" ) {
          bounds = [that.projection([-5.269492, 51.195627]), that.projection([10.462930, 41.528031])];
        } else {
          bounds = that.path.bounds(feature);
        }
      }
    } else if (id) {
      var minLat, maxLat, minLon, maxLon;

      for (i in that.placemarks) {
        var lat = that.placemarks[i].latLng[0];
        var lon = that.placemarks[i].latLng[1];
        if (!minLat || lat < minLat) {
          minLat = lat;
        }
        if (!minLon || lon < minLon) {
          minLon = lon;
        }
        if (!maxLat || lat > maxLat) {
          maxLat = lat;
        }
        if (!maxLon || lon > maxLon) {
          maxLon = lon;
        }
      }

      if (minLon == maxLon) {
        minLon -= 7;
        maxLon += 7;
      }

      if (minLat == maxLat) {
        minLat -= 7;
        maxLat += 7;
      }

      bounds = [that.projection([minLon, minLat]), that.projection([maxLon, maxLat])];
    }
    
    if(bounds) {
      var dx = bounds[1][0] - bounds[0][0],
          dy = bounds[1][1] - bounds[0][1],
          x = (bounds[0][0] + bounds[1][0]) / 2,
          y = (bounds[0][1] + bounds[1][1]) / 2,
          scale = /* .5 /  */ 0.9 / Math.max(dx / that.width, dy / that.height),
          translate = [that.width / 2 - scale * x, that.height / 2 - scale * y];
      
      that.svg
        .transition()
        .duration(750)
        .call(that.zoom.translate(translate).scale(scale).event);
      //that.setView(translate, scale, that.height / 4);
    }
  },

  getMarkers : function(resource, labelCallback, origin) {

    origin = origin || resource;
    var that = this;
    var locations = [];
    var markers = [];

    if (resource.location && resource.location instanceof Array) {
      locations = locations.concat(resource.location);
    } else if (resource.location) {
      locations.push(resource.location);
    }

    for (var l in locations) {
      if (geo = locations[l].geo) {
        markers.push({
          latLng: [geo['lat'], geo['lon']],
          name: labelCallback ? labelCallback(origin) : resource['@id'],
          url: "/resource/" + origin['@id']
        })
      }
    }

    if (!markers.length) {
      for (var key in resource) {
        var value = resource[key];
        if (value instanceof Array) {
          for (var i = 0; i < value.length; i++) {
            if (typeof value[i] == 'object') {
              markers = markers.concat(
                that.getMarkers(value[i], labelCallback, origin)
              );
            }
          }
        } else if (typeof value == 'object') {
          markers = markers.concat(
            that.getMarkers(value, labelCallback, origin)
          );
        }
      }
    }

    return markers;

  },

  getResourceLabel : function(resource) {
    switch (resource['@type']) {
      case 'Person':
        return resource['name'][0]['@value'];
      case 'Organization':
        return resource['legalName']['@value'];
      case 'Article':
        return resource['name'][0]['@value'];
      default:
        return resource['@id'];
    }
  }

};
