/**
 * The Class class
 *
 * Copyright (c) 2008, Digg, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, 
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, 
 *   this list of conditions and the following disclaimer in the documentation 
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Digg, Inc. nor the names of its contributors 
 *   may be used to endorse or promote products derived from this software 
 *   without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @module Class
 * @author Micah Snyder <micah@digg.com>
 * @description Class creation and management for use with jQuery
 * @link http://code.google.com/p/digg
 *
 * @requires Array.indexOf -- If you support older browsers, make sure you prototype this in
 */


(function($){Class={create:function(){var s=(arguments.length>0&&arguments[arguments.length-1].constructor==Boolean)?arguments[arguments.length-1]:false;var c=s?{}:function(){this.init.apply(this,arguments);}
var methods={ns:[],supers:{},init:function(){},namespace:function(ns){if(!ns)return null;var _this=this;if(ns.constructor==Array){$.each(ns,function(){_this.namespace.apply(_this,[this]);});return;}else if(ns.constructor==Object){for(var key in ns){if([Object,Function].indexOf(ns[key].constructor)>-1){if(!this.ns)this.ns=[];this.ns[key]=ns[key];this.namespace.apply(this,[key]);}}
return;}
var levels=ns.split(".");var nsobj=this.prototype?this.prototype:this;$.each(levels,function(){nsobj[this]=_this.ns[this]||nsobj[this]||window[this]||Class.create(true);delete _this.ns[this];nsobj=nsobj[this];});return nsobj;},create:function(){var args=Array.prototype.slice.call(arguments);var name=args.shift();var temp=Class.create.apply(Class,args);var ns={};ns[name]=temp;this.namespace(ns);},sup:function(){try{var caller=this.sup.caller.name;this.supers[caller].apply(this,arguments);}catch(noSuper){return false;}}}
s?delete methods.init:null;$.extend(c,methods);if(!s)$.extend(c.prototype,methods);var extendee=s?c:c.prototype;$.each(arguments,function(){if(this.constructor==Object||typeof this.init!=undefined){for(i in this){if(extendee[i]&&extendee[i].constructor==Function&&['namespace','create','sup'].indexOf(i)==-1){this[i].name=extendee[i].name=i;extendee.supers[i]=extendee[i];}
extendee[i]=this[i];}}});return c;}};})(jQuery);
