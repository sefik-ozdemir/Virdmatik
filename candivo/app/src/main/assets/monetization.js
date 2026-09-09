(function(){
"use strict";
const ADS_KEY="candivo-ads-break-v1";
const REWARD_COOLDOWN_KEY="candivo-reward-cooldown-v1";
const BREAK_COOLDOWN_MS=120000;
const REWARD_COOLDOWN_MS=30000;
let breakCount=0,lastBreak=0,rewardPending="",rewardPausedTimer=false;
try{const s=JSON.parse(localStorage.getItem(ADS_KEY)||"{}");breakCount=Number(s.count)||0;lastBreak=Number(s.last)||0;}catch(error){}
function nativeAds(){return !!(window.CandivoAndroid&&typeof window.CandivoAndroid.showRewarded==="function");}
function saveBreak(){try{localStorage.setItem(ADS_KEY,JSON.stringify({count:breakCount,last:lastBreak}));}catch(error){}}
function rewardName(kind){return ({moves5:"+5 hamle / +15 sn",shuffle:"Karıştır",bomb:"Bomba",stripe:"Lazer"})[kind]||"Ücretsiz güç";}
function canReward(){
  if(!nativeAds())return false;
  if(typeof duel3!=="undefined"&&duel3.active){toast3("Düello sırasında reklam ödülü kullanılamaz.");return false;}
  if(typeof busy3!=="undefined"&&busy3){toast3("Hamle animasyonu bitince reklam ödülünü kullanabilirsin.");return false;}
  let last=0;try{last=Number(localStorage.getItem(REWARD_COOLDOWN_KEY))||0;}catch(error){}
  if(Date.now()-last<REWARD_COOLDOWN_MS){toast3("Yeni ödüllü reklam için kısa bir süre bekle.");return false;}
  return true;
}
function requestReward(kind){
  if(!canReward())return;
  rewardPending=kind;
  rewardPausedTimer=!!(typeof levelSessionActive3!=="undefined"&&levelSessionActive3&&typeof currentConfig3!=="undefined"&&currentConfig3&&currentConfig3.mode==="time");
  if(rewardPausedTimer&&typeof stopTimer3==="function")stopTimer3();
  try{window.CandivoAndroid.showRewarded(kind);}catch(error){adError("Reklam şu anda açılamadı.");}
}
function grantReward(kind){
  if(!kind||kind!==rewardPending)return;
  rewardPending="";
  try{localStorage.setItem(REWARD_COOLDOWN_KEY,String(Date.now()));}catch(error){}
  if(typeof levelSessionActive3!=="undefined"&&levelSessionActive3&&!duel3.active){
    if(kind==="moves5"){
      if(currentConfig3&&currentConfig3.mode==="time")timeLeft3+=15;else moves3+=5;
      toast3("🎁 Reklam ödülü: "+rewardName(kind));
    }else if(kind==="shuffle"){
      shuffleBoard3(false);toast3("🎁 Reklam ödülü: tahta karıştırıldı.");
    }else if(kind==="bomb"){
      placeRandomSpecial3("bomb");render3();toast3("🎁 Reklam ödülü: bomba eklendi.");
    }else if(kind==="stripe"){
      placeRandomSpecial3(Math.random()<.5?"h":"v");render3();toast3("🎁 Reklam ödülü: lazer eklendi.");
    }
    updateStats3();saveProgress3();return;
  }
  const meta={moves5:["Ekstra Hamle","⏱️",20],shuffle:["Karıştırıcı","🔀",15],bomb:["Renk Bombası","🌈",40],stripe:["Lazer Meyve","⚡",30]}[kind];
  if(meta&&typeof inventory3!=="undefined"){
    invId3++;inventory3.push({id:invId3,kind:kind,name:meta[0],icon:meta[1],sellValue:meta[2]});renderInventory3();saveProgress3();toast3("🎁 Ücretsiz güç envanterine eklendi.");
  }
}
function resumeAfterRewardAd(){if(rewardPausedTimer&&typeof levelSessionActive3!=="undefined"&&levelSessionActive3&&typeof duel3!=="undefined"&&!duel3.active&&typeof startTimer3==="function")startTimer3();rewardPausedTimer=false;}
function adError(message){rewardPending="";resumeAfterRewardAd();if(typeof toast3==="function")toast3(message||"Reklam şu anda hazır değil. Biraz sonra tekrar dene.");}
function maybeInterstitial(reason){
  if(!nativeAds())return;
  const now=Date.now();breakCount++;
  if(breakCount<3||now-lastBreak<BREAK_COOLDOWN_MS){saveBreak();return;}
  breakCount=0;lastBreak=now;saveBreak();
  setTimeout(function(){if((typeof levelSessionActive3!=="undefined"&&levelSessionActive3)||(typeof duel3!=="undefined"&&duel3.active))return;try{window.CandivoAndroid.showInterstitial(reason||"natural_break");}catch(error){}},650);
}
function isNaturalBreakTitle(title){
  return /Seviye .*tamamlandı|Düelloyu Kazandın|Bu kez rakip kazandı|Düello Berabere|Düellodan pes ettin|Rakibin pes etti|Bağlantın kesildi|Rakibin bağlantısı kesildi/i.test(title||"");
}
function injectRewardShop(){
  const panel=document.querySelector("#lowerPanel .panel-content");if(!panel||document.getElementById("rewardAdsBox3"))return;
  const box=document.createElement("div");box.className="shop";box.id="rewardAdsBox3";box.style.display=nativeAds()?"block":"none";
  box.innerHTML='<h2>🎁 Reklam İzle, Ücretsiz Güç</h2><p style="opacity:.82;margin:.2rem 0 .65rem">Elmas harcamadan bir güç kazan. Düelloda kullanılamaz.</p><div class="shop-grid"><button class="shop-btn" data-reward-ad="moves5"><span class="icon">🎬</span>+5 Hamle / +15 sn<span class="price">Reklam izle</span></button><button class="shop-btn" data-reward-ad="shuffle"><span class="icon">🎬</span>Karıştır<span class="price">Reklam izle</span></button><button class="shop-btn" data-reward-ad="bomb"><span class="icon">🎬</span>Bomba Ekle<span class="price">Reklam izle</span></button><button class="shop-btn" data-reward-ad="stripe"><span class="icon">🎬</span>Lazer Ekle<span class="price">Reklam izle</span></button></div>';
  box.querySelectorAll("[data-reward-ad]").forEach(function(b){b.onclick=function(){requestReward(b.dataset.rewardAd);};});
  panel.insertBefore(box,panel.firstChild);
}
function injectPrivacyButton(){
  const observer=new MutationObserver(function(records){
    records.forEach(function(record){record.addedNodes.forEach(function(node){
      if(!(node instanceof HTMLElement))return;
      const title=node.querySelector&&node.querySelector(".card h2");
      if(title&&isNaturalBreakTitle(title.textContent))maybeInterstitial(title.textContent);
      const legal=node.querySelector&&node.querySelector("#legalBtn");
      if(legal&&nativeAds()&&!node.querySelector("#adPrivacyBtn3")){
        const b=document.createElement("button");b.type="button";b.id="adPrivacyBtn3";b.className="secondary";b.textContent="🍪 Reklam Gizlilik Tercihleri";
        b.onclick=function(){try{window.CandivoAndroid.openPrivacyOptions();}catch(error){toast3("Gizlilik seçenekleri şu anda açılamadı.");}};
        legal.insertAdjacentElement("afterend",b);
      }
    });});
  });
  observer.observe(document.body,{childList:true,subtree:false});
}
window.CandivoAds={
  onRewardEarned:grantReward,
  onRewardError:adError,
  onRewardClosed:resumeAfterRewardAd,
  onInterstitialError:function(){},
  onConsentError:function(message){if(message&&typeof console!=="undefined")console.warn("Consent:",message);},
  nativeAvailable:nativeAds,
  requestReward:requestReward,
  maybeInterstitial:maybeInterstitial
};
injectRewardShop();injectPrivacyButton();
})();
